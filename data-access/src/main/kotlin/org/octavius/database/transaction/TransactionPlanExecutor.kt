package org.octavius.database.transaction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataResult
import org.octavius.data.exception.DatabaseException
import org.octavius.data.exception.QueryExecutionException
import org.octavius.data.exception.StepDependencyException
import org.octavius.data.exception.TransactionStepExecutionException
import org.octavius.data.transaction.StepHandle
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionPlanResult
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
        private const val SCALAR_RESULT_KEY = "result"
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
    fun execute(plan: TransactionPlan): DataResult<TransactionPlanResult> {
        val stepsWithHandles = plan.steps // Pobieramy listę par (Handle, Step)
        if (stepsWithHandles.isEmpty()) {
            logger.debug { "Executing an empty transaction plan." }
            return DataResult.Success(TransactionPlanResult(emptyMap()))
        }

        // Krok 1: Stwórz mapę do szybkiego tłumaczenia uchwytów na indeksy
        val handleToIndexMap = stepsWithHandles.withIndex().associate { (index, pair) -> pair.first to index }

        // --- Walidacja (niepoprawna kolejność jest możliwa tylko za pomocą funkcji addPlan) ----
        for ((currentIndex, pair) in stepsWithHandles.withIndex()) {
            val step = pair.second
            for (paramValue in step.params.values) {
                if (paramValue is TransactionValue.FromStep) {
                    // Znajdź indeks kroku, od którego zależy ten parametr
                    val sourceIndex = handleToIndexMap[paramValue.handle]
                        ?: throw IllegalStateException("Validation failed: Found a handle that doesn't exist in the plan. This should never happen.")

                    // KLUCZOWY WARUNEK: Indeks źródła danych musi być mniejszy niż indeks bieżącego kroku.
                    if (sourceIndex >= currentIndex) {
                        throw StepDependencyException(
                            "Validation failed: Step $currentIndex attempts to use a result from a future or current step $sourceIndex.",
                            currentIndex
                        )
                    }
                }
            }
        }

        logger.info { "Executing transaction plan with ${stepsWithHandles.size} steps." }

        val transactionTemplate = TransactionTemplate(transactionManager)

        // Używamy `runCatching` do eleganckiej obsługi wyjątków wewnątrz transakcji
        val result: DataResult<TransactionPlanResult> = runCatching {

            // `transactionTemplate.execute` wykonuje logikę wewnątrz transakcji
            val finalResultsMap: Map<StepHandle<*>, Any?> = transactionTemplate.execute { transactionStatus ->
                val indexedResults = mutableMapOf<Int, Any?>()

                // Pętla po krokach. Używamy `withIndex` żeby mieć dostęp do `index`.
                for ((index, pair) in stepsWithHandles.withIndex()) {
                    val handle = pair.first
                    val step = pair.second

                    try {
                        logger.debug { "Executing step $index..." }

                        // Krok 2: Rozwiąż referencje do wyników poprzednich kroków
                        val resolvedParams = step.params.mapValues { (_, value) ->
                            resolveReference(value, indexedResults, handleToIndexMap)
                        }

                        // Krok 3: Zbuduj finalną mapę parametrów, "rozsmarowując" wyniki z `FromStep.Row`
                        val finalParams = mutableMapOf<String, Any?>()
                        resolvedParams.forEach { key, resolvedValue ->
                            val originalParam = step.params[key]
                            if (originalParam is TransactionValue.FromStep.Row && resolvedValue is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                finalParams.putAll(resolvedValue as Map<String, Any?>)
                            } else {
                                finalParams[key] = resolvedValue
                            }
                        }
                        logger.trace { "--> Final params for step $index: $finalParams" }

                        // Krok 4: Wykonaj logikę kroku
                        val stepResult = step.executionLogic(step.builder, finalParams)

                        // Krok 5: Obsłuż wynik kroku
                        when (stepResult) {
                            is DataResult.Success -> {
                                indexedResults[index] = stepResult.value
                            }
                            is DataResult.Failure -> {
                                // Jeśli krok zwrócił błąd, rzucamy go, aby przerwać transakcję
                                throw stepResult.error
                            }
                        }

                    } catch (e: Exception) {
                        // Krok 6: Opakuj KAŻDY błąd w kontekst kroku
                        throw TransactionStepExecutionException(
                            stepIndex = index,
                            failedStep = step, // Użyjemy TransactionStep<*> w definicji wyjątku
                            cause = e
                        )
                    }
                }

                // Krok 7: Po udanym wykonaniu wszystkich kroków, stwórz finalną mapę wyników (Handle -> Wynik)
                stepsWithHandles.associate { (handle, _) ->
                    val index = handleToIndexMap.getValue(handle)
                    handle to indexedResults[index]
                }

            }!! // `execute` zwraca nullable, ale w tym przypadku wiemy, że nie będzie nullem

            // Jeśli transakcja się powiodła, opakowujemy wynik w Success
            DataResult.Success(TransactionPlanResult(finalResultsMap))

        }.getOrElse { error ->
            // Jeśli `runCatching` złapał jakikolwiek wyjątek, opakowujemy go w Failure
            val dbException = when (error) {
                is DatabaseException -> error // Jeśli to już nasz wyjątek, przekaż go dalej
                else -> QueryExecutionException( // W przeciwnym razie stwórz nowy
                    "An unexpected error occurred during transaction execution.",
                    "N/A", emptyMap(), error
                )
            }
            logger.error(dbException) { "Transaction failed and was rolled back." }
            DataResult.Failure(dbException)
        }

        return result
    }

    private fun resolveReference(
        value: Any?,
        indexedResults: Map<Int, Any?>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Any? {
        if (value !is TransactionValue.FromStep) {
            return if (value is TransactionValue.Value) value.value else value
        }

        val stepIndex = handleToIndexMap[value.handle]
            ?: throw StepDependencyException("Cannot resolve reference: unknown StepHandle.", -1)

        val sourceResult: Any? = indexedResults[stepIndex]
            ?: throw StepDependencyException("Result for step $stepIndex not found.", stepIndex)

        return when (value) {
            is TransactionValue.FromStep.Field -> {
                val rowData = sourceResult.toRowMap(value.rowIndex, stepIndex)

                // Używamy "result" tylko jeśli columnName jest null
                val colName = value.columnName ?: SCALAR_RESULT_KEY

                if (!rowData.containsKey(colName)) {
                    val errorMsg = if (value.columnName == null) {
                        "Cannot resolve scalar value from step $stepIndex. The internal key 'result' was not found."
                    } else {
                        "Key '${value.columnName}' not found in result of step $stepIndex."
                    }
                    throw StepDependencyException(errorMsg, stepIndex, colName)
                }
                rowData[colName]
            }
            is TransactionValue.FromStep.Column -> {
                val sourceList = sourceResult as? List<*>
                    ?: throw StepDependencyException("Cannot extract a column. Result of step $stepIndex is not a List.", stepIndex)

                val columnValues: List<Any?> = if (value.columnName != null) {
                    // PRZYPADEK 1: Mamy nazwę kolumny -> wynik z toList()
                    @Suppress("UNCHECKED_CAST")
                    (sourceList as List<Map<String, Any?>>).map { row ->
                        if (!row.containsKey(value.columnName)) {
                            throw StepDependencyException("Key '${value.columnName}' not found in at least one row from step $stepIndex.", stepIndex, value.columnName)
                        }
                        row[value.columnName]
                    }
                } else {
                    // PRZYPADEK 2: Nie ma nazwy kolumny -> wynik z toColumn()
                    sourceList
                }

                if (value.asTypedArray) columnValues.toTypedArray() else columnValues
            }

            is TransactionValue.FromStep.Row -> {
                sourceResult.toRowMap(value.rowIndex, stepIndex)
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun Any?.toRowMap(rowIndex: Int, stepIndex: Int): Map<String, Any?> {
        return when (this) {
            is List<*> -> {
                if (rowIndex >= this.size) throw StepDependencyException("Cannot access row at index $rowIndex...", stepIndex)
                this[rowIndex] as? Map<String, Any?> ?: throw StepDependencyException("Result of step $stepIndex is a List, but its elements are not Maps.", stepIndex)
            }
            is Map<*, *> -> {
                if (rowIndex > 0) throw StepDependencyException("Cannot access row at index $rowIndex...", stepIndex)
                this as Map<String, Any?>
            }
            null -> throw StepDependencyException("Cannot extract data from a null result of step $stepIndex.", stepIndex)
            else -> { // Skalary (np. z `execute()` lub `toField()`)
                if (rowIndex > 0) throw StepDependencyException("Cannot access row at index $rowIndex...", stepIndex)
                mapOf(SCALAR_RESULT_KEY to this)
            }
        }
    }
}