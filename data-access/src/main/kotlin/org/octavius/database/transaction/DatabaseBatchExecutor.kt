package org.octavius.database.transaction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.DataResult
import org.octavius.data.contract.transaction.BatchExecutor
import org.octavius.data.contract.transaction.BatchStepResults
import org.octavius.data.contract.transaction.DatabaseValue
import org.octavius.data.contract.transaction.TransactionStep
import org.octavius.exception.BatchStepExecutionException
import org.octavius.exception.DatabaseException
import org.octavius.exception.QueryExecutionException
import org.octavius.exception.StepDependencyException
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
 *
 * @see TransactionStep
 * @see DatabaseValue
 * @see org.octavius.database.type.KotlinToPostgresConverter
 */
class DatabaseBatchExecutor(
    private val transactionManager: DataSourceTransactionManager,
) : BatchExecutor {
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
    override fun execute(transactionSteps: List<TransactionStep<*>>): DataResult<BatchStepResults> {
        logger.info { "Executing batch of ${transactionSteps.size} steps in a single transaction." }

        return try {
            val transactionTemplate = TransactionTemplate(transactionManager)

            val results: BatchStepResults = transactionTemplate.execute { status ->
                val allResults = mutableMapOf<Int, List<Map<String, Any?>>>()

                try {
                    for ((index, operation) in transactionSteps.withIndex()) {
                        try {
                            logger.debug { "Executing step $index/${transactionSteps.size - 1}: ${operation.builderState::class.simpleName}" }

                            val result: List<Map<String, Any?>>

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
                                            result = data as List<Map<String, Any?>>
                                        }

                                        is Map<*, *> -> {
                                            // Jeśli wynik to pojedyncza mapa, opakuj w listę
                                            @Suppress("UNCHECKED_CAST")
                                            result = listOf(data as Map<String, Any?>)
                                        }

                                        else -> {
                                            // Dla innych typów, stwórz mapę z wartością
                                            result = listOf(mapOf("result" to data))
                                        }
                                    }
                                }

                                is DataResult.Failure -> {
                                    throw buildResult.error
                                }
                            }
                            allResults[index] = result
                        } catch (e: DatabaseException) { // Niemożliwy jest inny wyjątek
                            throw BatchStepExecutionException(
                                stepIndex = index,
                                failedStep = operation,
                                cause = e
                            )
                        }
                    }
                } catch (e: Exception) { // Ten blok łapie wyjątki z całej pętli
                    status.setRollbackOnly()
                    logger.error(e) { "Error during transaction execution, rolling back. Failed step details in exception." }
                    throw e // Rzucamy dalej (teraz będzie to BatchStepExecutionException)
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
            DataResult.Failure(
                QueryExecutionException(
                    "An unexpected error occurred during batch execution.",
                    sql = "N/A",
                    params = emptyMap(),
                    cause = e
                )
            )
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
}