package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Wykonuje serię operacji bazodanowych w pojedynczej, atomowej transakcji.
 *
 * Główne cechy:
 * - **Atomowość**: Wszystkie operacje kończą się sukcesem lub żadna (rollback).
 * - **Zależności**: Wyniki z jednego kroku mogą być użyte jako parametry w kolejnych.
 * - **Ekspansja parametrów**: Automatyczne przekształcanie złożonych typów Kotlin
 *   (listy, data class, enumy) na natywne konstrukcje PostgreSQL.
 * - **Obsługa RETURNING**: Pobiera wartości wygenerowane przez bazę danych.
 *
 * @param transactionManager Menedżer transakcji Spring.
 * @param namedParameterJdbcTemplate Template do wykonywania zapytań SQL.
 * @param kotlinToPostgresConverter Konwerter złożonych typów Kotlin na SQL.
 *
 * @see DatabaseStep
 * @see DatabaseValue
 * @see org.octavius.database.type.KotlinToPostgresConverter
 */
class DatabaseBatchExecutor(
    private val transactionManager: DataSourceTransactionManager,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
): BatchExecutor {
    private val logger = KotlinLogging.logger {}
    /**
     * Wykonuje listę kroków bazodanowych w pojedynczej transakcji.
     *
     * W przypadku błędu w dowolnym kroku, cała transakcja jest wycofywana.
     *
     * @param databaseSteps Lista kroków (Insert, Update, Delete, RawSql) do wykonania.
     * @return Mapa, gdzie kluczem jest indeks kroku, a wartością lista zwróconych wierszy.
     * @throws Exception gdy którykolwiek krok się nie powiedzie.
     *
     * Przykład z zależnościami:
     * ```kotlin
     * val steps = listOf(
     *     DatabaseStep.Insert("users", mapOf("name" to "John".toDatabaseValue()), returning = listOf("id")),
     *     DatabaseStep.Insert("profiles", mapOf("user_id" to DatabaseValue.FromStep(0, "id")))
     * )
     * val results = batchExecutor.execute(steps)
     * ```
     */
    override fun execute(databaseSteps: List<DatabaseStep>): Map<Int, List<Map<String, Any?>>> {
        logger.info { "Executing batch of ${databaseSteps.size} steps in a single transaction." }
        val transactionTemplate = TransactionTemplate(transactionManager)
        val allResults = mutableMapOf<Int, List<Map<String, Any?>>>()

        transactionTemplate.execute { status ->
            try {
                for ((index, operation) in databaseSteps.withIndex()) {
                    val (sql, params) = buildQuery(operation, allResults)
                    val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

                    logger.debug { "Executing step $index: ${operation::class.simpleName}" }
                    logger.trace { "--> SQL: ${expanded.expandedSql}" }
                    logger.trace { "--> Params: ${expanded.expandedParams}" }

                    val result: List<Map<String, Any?>> = if (operation.returning.isNotEmpty()) {
                        // Używamy query, bo update z returning w Spring JDBC jest kłopotliwy
                        // To jest standardowy sposób na obejście tego problemu
                        namedParameterJdbcTemplate.queryForList(expanded.expandedSql, expanded.expandedParams)
                    } else {
                        val rowsAffected = namedParameterJdbcTemplate.update(expanded.expandedSql, expanded.expandedParams)
                        listOf(mapOf("rows_affected" to rowsAffected)) // Zwracamy info o zmienionych wierszach
                    }

                    allResults[index] = result
                }
            } catch (e: Exception) {
                status.setRollbackOnly()
                logger.error(e) { "Error executing transaction. Rolling back transaction." }
                throw e
            }
        }
        logger.info { "Batch execution completed successfully." }
        return allResults
    }

    /**
     * Rozwiązuje referencję `DatabaseValue` na konkretną wartość.
     *
     * - `DatabaseValue.Value`: Zwraca przechowywaną wartość.
     * - `DatabaseValue.FromStep`: Pobiera wartość z wyniku poprzedniego kroku.
     *
     * @param ref Referencja do rozwiązania.
     * @param resultsContext Wyniki z poprzednich kroków transakcji.
     * @return Rozwiązana wartość.
     * @throws IllegalStateException, jeśli referencja jest nieprawidłowa.
     */
    private fun resolveReference(ref: DatabaseValue, resultsContext: Map<Int, List<Map<String, Any?>>>): Any? {
        return when (ref) {
            is DatabaseValue.Value -> ref.value
            is DatabaseValue.FromStep -> {
                val previousResultList = resultsContext[ref.stepIndex]
                    ?: throw IllegalStateException("Nie znaleziono wyniku dla kroku o indeksie ${ref.stepIndex}")
                if (previousResultList.isEmpty()) {
                    throw IllegalStateException("Krok ${ref.stepIndex} nie zwrócił żadnych wierszy.")
                }
                // Domyślnie bierzemy wartość z pierwszego zwróconego wiersza
                previousResultList.first()[ref.resultKey]
                    ?: throw IllegalStateException("Klucz '${ref.resultKey}' nie został znaleziony w wyniku operacji ${ref.stepIndex}")
            }
        }
    }

    /**
     * Buduje zapytanie SQL i mapę parametrów na podstawie operacji `DatabaseStep`.
     *
     * @param op Operacja do przekształcenia.
     * @param resultsContext Kontekst wyników do rozwiązywania referencji.
     * @return Para zawierająca zapytanie SQL i mapę parametrów.
     */
    private fun buildQuery(op: DatabaseStep, resultsContext: Map<Int, List<Map<String, Any?>>>): Pair<String, Map<String, Any?>> {
        when (op) {
            is DatabaseStep.Insert -> {
                val params = op.data.mapValues { resolveReference(it.value, resultsContext) }
                val columns = params.keys.joinToString(", ")
                val placeholders = params.keys.joinToString(", ") { ":$it" }
                val returningClause = if (op.returning.isNotEmpty()) " RETURNING ${op.returning.joinToString(", ")}" else ""
                val sql = "INSERT INTO ${op.tableName} ($columns) VALUES ($placeholders)$returningClause"
                return sql to params
            }
            is DatabaseStep.Update -> {
                val dataParams = op.data.mapValues { resolveReference(it.value, resultsContext) }
                val filterParams = op.filter.mapValues { resolveReference(it.value, resultsContext) }

                val setClause = dataParams.keys.joinToString(", ") { "$it = :$it" }
                val whereClause = if (filterParams.isNotEmpty()) " WHERE " + filterParams.keys.joinToString(" AND ") { "$it = :$it" } else ""
                val returningClause = if (op.returning.isNotEmpty()) " RETURNING ${op.returning.joinToString(", ")}" else ""

                val sql = "UPDATE ${op.tableName} SET $setClause$whereClause$returningClause"
                return sql to (dataParams + filterParams)
            }
            is DatabaseStep.Delete -> {
                val filterParams = op.filter.mapValues { resolveReference(it.value, resultsContext) }
                val whereClause = if (filterParams.isNotEmpty()) " WHERE " + filterParams.keys.joinToString(" AND ") { "$it = :$it" } else ""
                val returningClause = if (op.returning.isNotEmpty()) " RETURNING ${op.returning.joinToString(", ")}" else ""

                val sql = "DELETE FROM ${op.tableName}$whereClause$returningClause"
                return sql to filterParams
            }
            is DatabaseStep.RawSql -> {
                val params = op.params.mapValues { resolveReference(it.value, resultsContext) }
                return op.sql to params
            }
        }
    }
}