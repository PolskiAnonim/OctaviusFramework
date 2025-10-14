package org.octavius.database.transaction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataResult
import org.octavius.data.exception.TransactionStepExecutionException
import org.octavius.data.exception.DatabaseException
import org.octavius.data.exception.QueryExecutionException
import org.octavius.data.exception.StepDependencyException
import org.octavius.data.transaction.TransactionPlanResults
import org.octavius.data.transaction.TransactionStep
import org.octavius.data.transaction.TransactionValue
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
 * @see TransactionValue
 * @see org.octavius.database.type.KotlinToPostgresConverter
 */
internal class TransactionPlanExecutor(
    private val transactionManager: DataSourceTransactionManager,
) {
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
     *     TransactionStep.Insert("users", mapOf("name" to "John".toTransactionValue()), returning = listOf("id")),
     *     TransactionStep.Insert("profiles", mapOf("user_id" to TransactionValue.FromStep(0, "id")))
     * )
     * val results = batchExecutor.execute(steps)
     * ```
     */
    fun execute(transactionSteps: List<TransactionStep<*>>): DataResult<TransactionPlanResults> {
        logger.info { "Executing batch of ${transactionSteps.size} steps in a single transaction." }

        return try {
            val transactionTemplate = TransactionTemplate(transactionManager)

            val results: TransactionPlanResults = transactionTemplate.execute { status ->
                val allResults = mutableMapOf<Int, List<Map<String, Any?>>>()

                try {
                    for ((index, step) in transactionSteps.withIndex()) {
                        try {
                            logger.debug { "Executing step $index/${transactionSteps.size - 1}: ${step.builderState::class.simpleName}" }

                            // Krok 1: Rozwiąż wszystkie referencje do wartości
                            val resolvedValues = step.params.mapValues { (_, value) ->
                                resolveReference(value, allResults)
                            }

                            // Krok 2: Zbuduj finalną mapę parametrów, obsługując "rozsmarowanie" wierszy (FromRow)
                            val finalParams = mutableMapOf<String, Any?>()
                            resolvedValues.forEach { key, resolvedValue ->
                                val originalParam = step.params[key]
                                if (originalParam is TransactionValue.FromStep.Row && resolvedValue is Map<*, *>) {
                                    // Jeśli parametr był typu Row i wynikiem jest mapa,
                                    // dodaj jej zawartość do głównych parametrów
                                    @Suppress("UNCHECKED_CAST")
                                    finalParams.putAll(resolvedValue as Map<String, Any?>)
                                } else {
                                    // W przeciwnym razie, dodaj normalnie
                                    finalParams[key] = resolvedValue
                                }
                            }

                            logger.trace { "--> Final params for step $index: $finalParams" }

                            val resultList: List<Map<String,Any?>>
                            // Krok 3: Wykonaj krok z finalnymi parametrami
                            val buildResult = step.terminalMethod(finalParams)
                            when (buildResult) {
                                is DataResult.Success -> {
                                    @Suppress("UNCHECKED_CAST") // Fakt że klucze to String wynika z builderów
                                    resultList = when (val data = buildResult.value) {
                                        is List<*> -> data as List<Map<String, Any?>>
                                        is Map<*, *> -> listOf(data as Map<String, Any?>)
                                        else -> listOf(mapOf("result" to data))
                                    }
                                }
                                is DataResult.Failure -> {
                                    throw buildResult.error
                                }
                            }
                            allResults[index] = resultList
                        } catch (e: DatabaseException) { // Niemożliwy jest inny wyjątek
                            throw TransactionStepExecutionException(stepIndex = index, failedStep = step, cause = e)
                        }
                    }
                } catch (e: Exception) { // Ten blok łapie wyjątki z całej pętli
                    status.setRollbackOnly()
                    logger.error(e) { "Error during transaction execution, rolling back. Failed step details in exception." }
                    throw e // Rzucamy dalej (teraz będzie to TransactionStepExecutionException)
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

    private fun resolveReference(value: Any?, resultsContext: TransactionPlanResults): Any? {
        // Jeśli wartość nie jest TransactionValue, po prostu ją zwróć
        if (value !is TransactionValue) return value

        return when (value) {
            is TransactionValue.Value -> value.value
            is TransactionValue.FromStep.Field -> {
                logger.trace { "Resolving Field from step ${value.stepIndex}, key '${value.columnName}', row ${value.rowIndex}" }
                val sourceResult = getResultList(resultsContext, value.stepIndex)

                if (sourceResult.size <= value.rowIndex) {
                    throw StepDependencyException(
                        message = "Cannot resolve Field: Step ${value.stepIndex} returned only ${sourceResult.size} rows, but tried to access index ${value.rowIndex}.",
                        referencedStepIndex = value.stepIndex
                    )
                }
                val row = sourceResult[value.rowIndex]
                if (!row.containsKey(value.columnName)) {
                    throw StepDependencyException(
                        message = "Cannot resolve Field: Key '${value.columnName}' not found in result of step ${value.stepIndex}.",
                        referencedStepIndex = value.stepIndex,
                        missingKey = value.columnName
                    )
                }
                row[value.columnName]
            }
            is TransactionValue.FromStep.Column -> {
                logger.trace { "Resolving Column from step ${value.stepIndex}, key '${value.columnName}'" }
                val sourceResult = getResultList(resultsContext, value.stepIndex)

                val column = sourceResult.map { row ->
                    if (!row.containsKey(value.columnName)) {
                        throw StepDependencyException(
                            message = "Cannot resolve Column: Key '${value.columnName}' not found in at least one row from step ${value.stepIndex}.",
                            referencedStepIndex = value.stepIndex,
                            missingKey = value.columnName
                        )
                    }
                    row[value.columnName]
                }
                if (value.asTypedArray)
                    column.toTypedArray()
                else
                    column
            }
            is TransactionValue.FromStep.Row -> {
                logger.trace { "Resolving Row from step ${value.stepIndex}, row ${value.rowIndex}" }
                val sourceResult = getResultList(resultsContext, value.stepIndex)

                if (sourceResult.size <= value.rowIndex) {
                    throw StepDependencyException(
                        message = "Cannot resolve Row: Step ${value.stepIndex} returned only ${sourceResult.size} rows, but tried to access index ${value.rowIndex}.",
                        referencedStepIndex = value.stepIndex
                    )
                }
                sourceResult[value.rowIndex]
            }
        }
    }

    private fun getResultList(resultsContext: TransactionPlanResults, stepIndex: Int): List<Map<String, Any?>> {
        return resultsContext[stepIndex]
            ?: throw StepDependencyException(
                message = "Cannot resolve reference: Result for step $stepIndex not found. It may not have been executed yet.",
                referencedStepIndex = stepIndex
            )
    }
}