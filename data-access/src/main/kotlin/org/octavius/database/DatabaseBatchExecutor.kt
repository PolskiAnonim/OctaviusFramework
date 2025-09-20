package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.*
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.exception.DatabaseException
import org.octavius.exception.QueryExecutionException
import org.octavius.exception.StepDependencyException
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
 * @see TransactionStep
 * @see DatabaseValue
 * @see org.octavius.database.type.KotlinToPostgresConverter
 */
class DatabaseBatchExecutor(
    private val transactionManager: DataSourceTransactionManager,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
): BatchExecutor {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Wykonuje listę kroków bazodanowych w pojedynczej transakcji.
     *
     * W przypadku błędu w dowolnym kroku, cała transakcja jest wycofywana.
     *
     * @param transactionSteps Lista kroków (Insert, Update, Delete, RawSql) do wykonania.
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
    override fun execute(transactionSteps: List<TransactionStep>): DataResult<BatchStepResults> {
        logger.info { "Executing batch of ${transactionSteps.size} steps in a single transaction." }

        return try {
            val transactionTemplate = TransactionTemplate(transactionManager)

            val results: BatchStepResults = transactionTemplate.execute { status ->
                val allResults = mutableMapOf<Int, List<Map<String, Any?>>>()

                try {
                    for ((index, operation) in transactionSteps.withIndex()) {
                        var expandedSql: String? = null
                        var expandedParams: Map<String, Any?>? = null
                        try {
                            logger.debug { "Executing step $index/${transactionSteps.size-1}: ${operation::class.simpleName}" }

                            val result: List<Map<String, Any?>> = when (operation) {
                                is TransactionStep.FromBuilder<*> -> {
                                    // Rozwiązuj referencje w parametrach
                                    val resolvedParams = operation.params.mapValues { (_, value) ->
                                        when (value) {
                                            is DatabaseValue -> resolveReference(value, allResults)
                                            else -> value
                                        }
                                    }

                                    logger.trace { "--> FromBuilder params: $resolvedParams" }

                                    // Wywołaj metodę terminalną buildera
                                    val buildResult = operation.terminalMethod(resolvedParams)
                                    when (buildResult) {
                                        is DataResult.Success -> {
                                            val data = buildResult.value
                                            when (data) {
                                                is List<*> -> {
                                                    // Jeśli wynik to już lista map, użyj jej
                                                    @Suppress("UNCHECKED_CAST")
                                                    data as List<Map<String, Any?>>
                                                }
                                                is Map<*, *> -> {
                                                    // Jeśli wynik to pojedyncza mapa, opakuj w listę
                                                    @Suppress("UNCHECKED_CAST")
                                                    listOf(data as Map<String, Any?>)
                                                }
                                                else -> {
                                                    // Dla innych typów, stwórz mapę z wartością
                                                    listOf(mapOf("result" to data))
                                                }
                                            }
                                        }
                                        is DataResult.Failure -> {
                                            throw buildResult.error
                                        }
                                    }
                                }
                                else -> {
                                    // Obsługa tradycyjnych kroków (Insert, Update, Delete)
                                    val (sql, params) = buildQuery(operation, allResults)
                                    val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
                                    expandedSql = expanded.expandedSql
                                    expandedParams = expanded.expandedParams

                                    logger.trace { "--> SQL: ${expanded.expandedSql}" }
                                    logger.trace { "--> Params: ${expanded.expandedParams}" }

                                    val returningColumns = when (operation) {
                                        is TransactionStep.Insert -> operation.returning
                                        is TransactionStep.Update -> operation.returning
                                        is TransactionStep.Delete -> operation.returning
                                        else -> emptyList()
                                    }

                                    if (returningColumns.isNotEmpty()) {
                                        val returnedRows: List<Map<String, Any?>> = namedParameterJdbcTemplate.query(
                                            expanded.expandedSql,
                                            expanded.expandedParams,
                                            rowMappers.ColumnNameMapper()
                                        )
                                        logger.debug { "Step $index executed, returned ${returnedRows.size} rows." }
                                        returnedRows
                                    } else {
                                        val rowsAffected = namedParameterJdbcTemplate.update(expanded.expandedSql, expanded.expandedParams)
                                        logger.debug { "Step $index executed, affected $rowsAffected rows." }
                                        listOf(mapOf("rows_affected" to rowsAffected))
                                    }
                                }
                            }

                            allResults[index] = result
                        } catch (e: Exception) {
                            // Ten wyjątek zostanie złapany niżej, ale rzucamy nowy z pełnym kontekstem.
                            throw QueryExecutionException(
                                message = "Failed to execute step $index: ${operation::class.simpleName}",
                                sql = expandedSql ?: "SQL not generated",
                                params = expandedParams ?: emptyMap(),
                                cause = e
                            )
                        }
                    }
                } catch (e: Exception) {
                    status.setRollbackOnly()
                    // Krytyczny błąd w transakcji, logujemy z oryginalnym wyjątkiem.
                    logger.error(e) { "Error during transaction execution, rolling back. Failed step details in exception." }
                    throw e // Rzucamy dalej, aby transactionTemplate go obsłużył.
                }

                allResults
            }!!

            logger.info { "Batch execution of ${transactionSteps.size} steps completed successfully." }
            DataResult.Success(results)

        } catch (e: DatabaseException) {
            // Łapiemy nasze specyficzne wyjątki rzucone z wewnątrz.
            logger.error(e) { "A database exception occurred during the batch execution. Transaction was rolled back." }
            DataResult.Failure(e)
        } catch (e: Exception) {
            // Łapiemy wszelkie inne, nieoczekiwane błędy.
            logger.error(e) { "An unexpected exception occurred during batch execution. Transaction was rolled back." }
            DataResult.Failure(QueryExecutionException(
                "An unexpected error occurred during batch execution.",
                sql = "N/A",
                params = emptyMap(),
                cause = e
            ))
        }
    }

    private fun resolveReference(ref: DatabaseValue, resultsContext: Map<Int, List<Map<String, Any?>>>): Any? {
        return when (ref) {
            is DatabaseValue.Value -> ref.value
            is DatabaseValue.FromStep -> {
                logger.trace { "Resolving reference from step ${ref.stepIndex}, key '${ref.resultKey}'" }
                val previousResultList = resultsContext[ref.stepIndex]
                    ?: throw StepDependencyException(
                        message = "Cannot resolve reference: Result for step ${ref.stepIndex} not found.",
                        referencedStepIndex = ref.stepIndex
                    )

                if (previousResultList.isEmpty()) {
                    throw StepDependencyException(
                        message = "Cannot resolve reference: Step ${ref.stepIndex} returned no rows.",
                        referencedStepIndex = ref.stepIndex
                    )
                }
                if (!previousResultList.first().containsKey(ref.resultKey)) {
                    throw StepDependencyException(
                        message = "Cannot resolve reference: Key '${ref.resultKey}' not found in result of step ${ref.stepIndex}.",
                        referencedStepIndex = ref.stepIndex,
                        missingKey = ref.resultKey
                    )
                }
                val resolvedValue = previousResultList.first()[ref.resultKey]
                logger.trace { "Reference resolved to value: $resolvedValue" }
                resolvedValue
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
    private fun buildQuery(op: TransactionStep, resultsContext: Map<Int, List<Map<String, Any?>>>): Pair<String, Map<String, Any?>> {
        when (op) {
            is TransactionStep.Insert -> {
                val params = op.data.mapValues { resolveReference(it.value, resultsContext) }
                val columns = params.keys.joinToString(", ")
                val placeholders = params.keys.joinToString(", ") { ":$it" }
                val returningClause = if (op.returning.isNotEmpty()) " RETURNING ${op.returning.joinToString(", ")}" else ""
                val sql = "INSERT INTO ${op.tableName} ($columns) VALUES ($placeholders)$returningClause"
                return sql to params
            }
            is TransactionStep.Update -> {
                val dataParams = op.data.mapValues { resolveReference(it.value, resultsContext) }
                val filterParams = op.filter.mapValues { resolveReference(it.value, resultsContext) }

                val setClause = dataParams.keys.joinToString(", ") { "$it = :$it" }
                val whereClause = if (filterParams.isNotEmpty()) " WHERE " + filterParams.keys.joinToString(" AND ") { "$it = :$it" } else ""
                val returningClause = if (op.returning.isNotEmpty()) " RETURNING ${op.returning.joinToString(", ")}" else ""

                val sql = "UPDATE ${op.tableName} SET $setClause$whereClause$returningClause"
                return sql to (dataParams + filterParams)
            }
            is TransactionStep.Delete -> {
                val filterParams = op.filter.mapValues { resolveReference(it.value, resultsContext) }
                val whereClause = if (filterParams.isNotEmpty()) " WHERE " + filterParams.keys.joinToString(" AND ") { "$it = :$it" } else ""
                val returningClause = if (op.returning.isNotEmpty()) " RETURNING ${op.returning.joinToString(", ")}" else ""

                val sql = "DELETE FROM ${op.tableName}$whereClause$returningClause"
                return sql to filterParams
            }

            is TransactionStep.FromBuilder<*> -> {
                throw UnsupportedOperationException() // FromBuilder nie używa tej metody
            }
        }
    }
}