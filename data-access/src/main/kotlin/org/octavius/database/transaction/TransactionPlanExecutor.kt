package org.octavius.database.transaction

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataResult
import org.octavius.data.exception.*
import org.octavius.data.transaction.*
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.TransactionDefinition
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

    fun execute(plan: TransactionPlan, propagation: TransactionPropagation): DataResult<TransactionPlanResult> {
        val stepsWithHandles = plan.steps // Pobieramy listę par (Handle, Step)
        if (stepsWithHandles.isEmpty()) {
            logger.debug { "Executing an empty transaction plan." }
            return DataResult.Success(TransactionPlanResult(emptyMap()))
        }
        return runCatching {
            // Krok 1: Stwórz mapę do szybkiego tłumaczenia uchwytów na indeksy
            val handleToIndexMap = stepsWithHandles.withIndex().associate { (index, pair) -> pair.first to index }

            // --- Walidacja (niepoprawna kolejność jest możliwa tylko za pomocą funkcji addPlan) ----
            for ((currentIndex, pair) in stepsWithHandles.withIndex()) {
                val step = pair.second
                for (paramValue in step.params.values) {
                    if (paramValue is TransactionValue.FromStep) {
                        // Znajdź indeks kroku, od którego zależy ten parametr
                        val sourceIndex = handleToIndexMap[paramValue.handle]
                            ?: throw StepDependencyException(StepDependencyExceptionMessage.UNKNOWN_STEP_HANDLE, currentIndex)

                        // KLUCZOWY WARUNEK: Indeks źródła danych musi być mniejszy niż indeks bieżącego kroku.
                        if (sourceIndex >= currentIndex) {
                            throw StepDependencyException(
                                StepDependencyExceptionMessage.DEPENDENCY_ON_FUTURE_STEP,
                                currentIndex,
                                sourceIndex
                            )
                        }
                    }
                }
            }

            logger.info { "Executing transaction plan with ${stepsWithHandles.size} steps." }

            val transactionTemplate = TransactionTemplate(transactionManager).apply {
                propagationBehavior = when (propagation) {
                    TransactionPropagation.REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED
                    TransactionPropagation.REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW
                    TransactionPropagation.NESTED -> TransactionDefinition.PROPAGATION_NESTED
                }
            }

            // `transactionTemplate.execute` wykonuje logikę wewnątrz transakcji
            val finalResultsMap: Map<StepHandle<*>, Any?> = transactionTemplate.execute {
                    val indexedResults = mutableMapOf<Int, Any?>()

                    // Pętla po krokach. Używamy `withIndex` żeby mieć dostęp do `index`.
                    for ((index, pair) in stepsWithHandles.withIndex()) {
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
                            val ex = TransactionStepExecutionException(
                                stepIndex = index,
                                cause = e
                            )
                            logger.error(ex) { ex }
                            throw ex
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
    }

    private fun resolveReference(
        value: Any?,
        indexedResults: Map<Int, Any?>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Any? {
        // 1. Jeśli to zwykła wartość, zwróć ją (istniejąca logika)
        if (value !is TransactionValue) {
            return value // np. String, Int
        }

        // 2. Jeśli to Value wrapper, odpakuj (istniejąca logika)
        if (value is TransactionValue.Value) {
            return value.value
        }

        // 3. Obsługa transformacji
        if (value is TransactionValue.Transformed) {
            // Najpierw pobierz "surową" wartość z wnętrza (rekurencja!)
            val rawValue = resolveReference(value.source, indexedResults, handleToIndexMap)

            // Zastosuj funkcję użytkownika
            return try {
                value.transform(rawValue)
            } catch (e: Exception) {
                // Musimy ustalić, jakiego kroku dotyczyła ta transformacja.
                // Ponieważ Transformed opakowuje inną wartość (np. FromStep),
                // musimy "dokopać się" do uchwytu, żeby podać poprawny stepIndex w błędzie.
                val rootHandle = extractRootHandle(value.source)
                val stepIndex = rootHandle?.let { handleToIndexMap[it] } ?: -1

                throw StepDependencyException(
                    messageEnum = StepDependencyExceptionMessage.TRANSFORMATION_FAILED,
                    referencedStepIndex = stepIndex,
                    args = arrayOf(e.message ?: e.toString()), // Przekazujemy tekst błędu jako argument
                    cause = e
                )
            }
        }

        value as TransactionValue.FromStep // Dla kompilatora

        val stepIndex = handleToIndexMap[value.handle]!! // Sprawdzane w początkowej walidacji

        // Logika pętli `execute` gwarantuje, że jeśli dotarliśmy do tego miejsca,
        // to krok `stepIndex` został wykonany, a jego wynik znajduje się w mapie.
        val sourceResult: Any? = indexedResults[stepIndex]

        // Jeśli wynik kroku źródłowego to null, każda referencja do niego też jest null.
        // To jest poprawne i zamierzone zachowanie.
        if (sourceResult == null) {
            return null
        }

        return when (value) {
            is TransactionValue.FromStep.Field -> {
                val rowData = sourceResult.toRowMap(value.rowIndex, stepIndex)

                // Używamy "result" tylko jeśli columnName jest null
                val colName = value.columnName ?: SCALAR_RESULT_KEY
                if (!rowData.containsKey(colName)) {
                    val errorMsg = if (value.columnName == null) {
                        StepDependencyExceptionMessage.SCALAR_NOT_FOUND
                    } else {
                        StepDependencyExceptionMessage.COLUMN_NOT_FOUND
                    }
                    throw StepDependencyException(errorMsg,stepIndex, colName)
                }
                rowData[colName]
            }
            is TransactionValue.FromStep.Column -> {
                // toList, toColumn, toListOf
                val sourceList = sourceResult as? List<*>
                    ?: throw StepDependencyException(StepDependencyExceptionMessage.RESULT_NOT_LIST,stepIndex)

                val columnValues: List<Any?> = if (value.columnName != null) {
                    @Suppress("UNCHECKED_CAST")
                    // Wynik toList
                    (sourceList as? List<Map<String, Any?>>)?.map { row ->
                        if (!row.containsKey(value.columnName)) {
                            throw StepDependencyException(
                                StepDependencyExceptionMessage.COLUMN_NOT_FOUND, stepIndex,
                                value.columnName!!
                            )
                        }
                        row[value.columnName]
                        // użycie nazwy kolumny na wyniku który jest listą skalarów
                    } ?: throw StepDependencyException(StepDependencyExceptionMessage.RESULT_NOT_MAP_LIST, stepIndex)
                } else {
                    // toColumn, toListOf
                    sourceList
                }

                // toListOf otrzyma błąd wewnątrz konwertera jeżeli jest tam data class niebędąca kompozytem
                return  columnValues
            }

            is TransactionValue.FromStep.Row -> {
                sourceResult.toRowMap(value.rowIndex, stepIndex)
            }
        }
    }

    // Pomocnicza funkcja, żeby znaleźć "winowajcę" (uchwyt) w głąb zagnieżdżeń
    private fun extractRootHandle(value: TransactionValue): StepHandle<*>? {
        return when (value) {
            is TransactionValue.FromStep -> value.handle
            is TransactionValue.Transformed -> extractRootHandle(value.source)
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    // Sygnatura `toRowMap` przyjmuje `Any`, bo `null` jest obsługiwany wcześniej w `resolveReference`.
    private fun Any.toRowMap(rowIndex: Int, stepIndex: Int): Map<String, Any?> {
        when (this) {
            // toList, toColumn, toListOf
            is List<*> -> {
                if (rowIndex >= this.size) {
                    throw StepDependencyException(StepDependencyExceptionMessage.ROW_INDEX_OUT_OF_BOUNDS, stepIndex, rowIndex, this.size)
                }
                val element = this[rowIndex]

                return when (element) {
                    // Wynik toList - ewentualnie może być zwrócona pusta mapa - brak nulli
                    is Map<*, *> -> element as Map<String, Any?>
                    // toListOf przechodzi - w konwerterze błąd dla niekompozytów
                    // albo toColumn - dany element może być nullem
                    else -> mapOf(SCALAR_RESULT_KEY to element)
                }
            }
            // toSingle
            is Map<*, *> -> {
                if (rowIndex > 0) {
                    throw StepDependencyException(StepDependencyExceptionMessage.INVALID_ROW_ACCESS_ON_NON_LIST, stepIndex, rowIndex)
                }
                return this as Map<String, Any?>
            }
            // toSingleOf, toField, execute
            else -> {
                if (rowIndex > 0) {
                    throw StepDependencyException(StepDependencyExceptionMessage.INVALID_ROW_ACCESS_ON_NON_LIST, stepIndex, rowIndex)
                }
                // Wynik toSingleOf (błąd w konwerterze i type registry gdy użyte na niekompozycie)
                // toField może być nullem, natomiast execute nie
                return mapOf(SCALAR_RESULT_KEY to this)
            }
        }
    }
}