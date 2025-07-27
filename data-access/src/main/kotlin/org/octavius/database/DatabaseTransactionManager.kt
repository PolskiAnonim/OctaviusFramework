package org.octavius.database

import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.data.contract.TransactionManager
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * Menedżer transakcji bazodanowych obsługujący atomowe operacje.
 *
 * Zapewnia transakcyjne wykonywanie listy operacji bazodanowych z obsługą:
 * - **Atomowość**: Wszystkie operacje wykonują się pomyślnie lub żadna (rollback)
 * - **Zależności**: Możliwość używania wyników poprzednich operacji w kolejnych krokach
 * - **Ekspansja parametrów**: Automatyczne przetwarzanie złożonych typów PostgreSQL
 * - **RETURNING**: Obsługa klauzuli RETURNING dla pobierania wygenerowanych wartości
 *
 * @param transactionManager Menedżer transakcji Spring do zarządzania transakcjami CUD
 * @param namedParameterJdbcTemplate Template JDBC do wykonywania zapytań z named parameters
 * @param parameterExpandHelper Helper do ekspansji złożonych parametrów PostgreSQL
 *
 * @see DatabaseStep
 * @see DatabaseValue
 * @see ParameterExpandHelper
 */
class DatabaseTransactionManager(
    private val transactionManager: DataSourceTransactionManager,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val parameterExpandHelper: ParameterExpandHelper
): TransactionManager {

    /**
     * Wykonuje listę kroków bazodanowych w pojedynczej transakcji.
     *
     * Deleguje do DatabaseTransactionManager, który obsługuje:
     * - Transakcyjność (wszystkie kroki lub żaden)
     * - Rozwiązywanie zależności między krokami (użycie wyniku jednego kroku w kolejnym)
     * - Rollback przy błędach
     * - Ekspansję złożonych parametrów PostgreSQL
     *
     * @param databaseSteps Lista kroków do wykonania.
     * @return Mapa wyników operacji.
     *
     * @throws Exception gdy którykolwiek krok się nie powiedzie (z rollback).
     *
     * Przykład z zależnościami:
     * ```kotlin
     * val steps = listOf(
     *     DatabaseStep.Insert("users", userData, returning = listOf("id")),
     *     DatabaseStep.Insert("profiles", mapOf(
     *         "user_id" to DatabaseValue.FromStep(0, "id"),
     *         "bio" to DatabaseValue.Value("Hello")
     *     ))
     * )
     * val results = DatabaseManager.getTransactionManager().execute(steps)
     * ```
     */
    override fun execute(databaseSteps: List<DatabaseStep>): Map<Int, List<Map<String, Any?>>> {
        val transactionTemplate = TransactionTemplate(transactionManager)
        val allResults = mutableMapOf<Int, List<Map<String, Any?>>>()

        transactionTemplate.execute { status ->
            try {
                for ((index, operation) in databaseSteps.withIndex()) {
                    val (sql, params) = buildQuery(operation, allResults)
                    val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

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
                println("Błąd operacji bazodanowej: ${e.message}")
                throw e
            }
        }
        return allResults
    }

    /**
     * Rozwiązuje referencje DatabaseValue na konkretne wartości.
     *
     * Konwertuje abstrakcyjne referencje na wartości gotowe do użycia w zapytaniach SQL.
     * Obsługuje dwa typy referencji:
     * - **DatabaseValue.Value**: Zwraca bezpośrednią wartość
     * - **DatabaseValue.FromStep**: Pobiera wartość z wyniku poprzedniej operacji
     *
     * @param ref Referencja do rozwiązania
     * @param resultsContext Kontekst wyników poprzednich operacji (indeks operacji → lista wyników)
     * @return Rozwiązana wartość
     * @throws IllegalStateException jeśli referencja wskazuje na nieistniejący wynik
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
     * Buduje zapytanie SQL i mapę parametrów dla operacji bazodanowej.
     *
     * Konwertuje abstrakcyjną operację DatabaseStep na konkretne zapytanie SQL
     * z odpowiednimi placeholderami i mapą parametrów. Obsługuje wszystkie
     * typy operacji: INSERT, UPDATE, DELETE, RawSql.
     *
     * @param op Operacja bazodanowa do przekształcenia
     * @param resultsContext Kontekst wyników do rozwiązywania referencji
     * @return Para zawierająca zapytanie SQL i mapę parametrów
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