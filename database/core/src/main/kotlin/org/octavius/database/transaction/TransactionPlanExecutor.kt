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
            val handleToIndexMap = validatePlan(stepsWithHandles)

            // Krok 2: Przygotowanie szablonu transakcji
            val transactionTemplate = createTransactionTemplate(propagation)

            val finalResultsMap = transactionTemplate.execute {
                executeStepsInTransaction(stepsWithHandles, handleToIndexMap)
            }

            // Jeśli transakcja się powiodła, opakowujemy wynik w Success
            DataResult.Success(TransactionPlanResult(finalResultsMap))

        }.getOrElse { error ->
            // Krok 4: Obsługa błędów na najwyższym poziomie
            handleTransactionError(error)
        }
    }

    private fun validateTransactionValue(
        value: Any?,
        currentIndex: Int,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ) {
        when (value) {
            is TransactionValue.FromStep -> {
                val sourceIndex = handleToIndexMap[value.handle]
                    ?: throw StepDependencyException(StepDependencyExceptionMessage.UNKNOWN_STEP_HANDLE, currentIndex)

                if (sourceIndex >= currentIndex) {
                    throw StepDependencyException(
                        StepDependencyExceptionMessage.DEPENDENCY_ON_FUTURE_STEP,
                        currentIndex,
                        sourceIndex
                    )
                }
            }
            is TransactionValue.Transformed -> {
                // Walidujemy to, co jest w środku.
                validateTransactionValue(value.source, currentIndex, handleToIndexMap)
            }
            // Inne typy (Value, zwykłe wartości) nie wymagają walidacji, więc nic nie robimy.
        }
    }

    private fun validatePlan(stepsWithHandles: List<Pair<StepHandle<*>, TransactionStep<*>>>): Map<StepHandle<*>, Int> {
        val handleToIndexMap = stepsWithHandles.withIndex().associate { (index, pair) -> pair.first to index }

        for ((currentIndex, pair) in stepsWithHandles.withIndex()) {
            val step = pair.second
            for (paramValue in step.params.values) {
                validateTransactionValue(paramValue, currentIndex, handleToIndexMap)
            }
        }

        logger.info { "Transaction plan validated successfully. Executing ${stepsWithHandles.size} steps." }
        return handleToIndexMap
    }

    private fun createTransactionTemplate(propagation: TransactionPropagation): TransactionTemplate {
        return TransactionTemplate(transactionManager).apply {
            propagationBehavior = when (propagation) {
                TransactionPropagation.REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED
                TransactionPropagation.REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW
                TransactionPropagation.NESTED -> TransactionDefinition.PROPAGATION_NESTED
            }
        }
    }

    private fun executeStepsInTransaction(
        stepsWithHandles: List<Pair<StepHandle<*>, TransactionStep<*>>>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Map<StepHandle<*>, Any?> {
        val indexedResults = mutableMapOf<Int, Any?>()

        for ((index, pair) in stepsWithHandles.withIndex()) {
            val step = pair.second
            try {
                executeSingleStep(index, step, indexedResults, handleToIndexMap)
            } catch (e: Exception) {
                // Opakuj KAŻDY błąd w kontekst kroku i rzuć go dalej, aby wycofać transakcję
                throw TransactionStepExecutionException(stepIndex = index, cause = e)
            }
        }

        // Po udanym wykonaniu wszystkich kroków, stwórz finalną mapę wyników
        return stepsWithHandles.associate { (handle, _) ->
            handle to indexedResults.getValue(handleToIndexMap.getValue(handle))
        }
    }

    private fun executeSingleStep(
        index: Int,
        step: TransactionStep<*>,
        indexedResults: MutableMap<Int, Any?>, // Modyfikujemy tę mapę
        handleToIndexMap: Map<StepHandle<*>, Int>
    ) {
        logger.debug { "Executing step $index..." }

        // Rozwiąż referencje i zbuduj finalne parametry
        val finalParams = buildFinalParameters(step, indexedResults, handleToIndexMap)
        logger.trace { "--> Final params for step $index: $finalParams" }

        // Wykonaj logikę kroku
        // Obsłuż wynik kroku
        when (val stepResult = step.executionLogic(step.builder, finalParams)) {
            is DataResult.Success -> {
                indexedResults[index] = stepResult.value
            }
            is DataResult.Failure -> {
                // Rzucamy błąd, zostanie złapany piętro wyżej i opakowany
                throw stepResult.error
            }
        }
    }

    private fun buildFinalParameters(
        step: TransactionStep<*>,
        indexedResults: Map<Int, Any?>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Map<String, Any?> {
        val resolvedParams = step.params.mapValues { (_, value) ->
            resolveReference(value, indexedResults, handleToIndexMap)
        }

        val finalParams = mutableMapOf<String, Any?>()
        resolvedParams.forEach { (key, resolvedValue) ->
            val originalParam = step.params[key]
            if (originalParam is TransactionValue.FromStep.Row && resolvedValue is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                finalParams.putAll(resolvedValue as Map<String, Any?>)
            } else {
                finalParams[key] = resolvedValue
            }
        }
        return finalParams
    }

    private fun handleTransactionError(error: Throwable): DataResult.Failure {
        val dbException = when (error) {
            is DatabaseException -> error
            else -> TransactionException(error)
        }
        logger.error(dbException) { "Transaction failed and was rolled back." }
        return DataResult.Failure(dbException)
    }


    private fun resolveReference(
        value: Any?,
        indexedResults: Map<Int, Any?>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Any? {
        if (value !is TransactionValue) {
            return value // Zwykła wartość
        }

        return when (value) {
            is TransactionValue.Value -> value.value // Odpakowanie
            is TransactionValue.Transformed -> resolveTransformed(value, indexedResults, handleToIndexMap)
            is TransactionValue.FromStep -> resolveFromStep(value, indexedResults, handleToIndexMap)
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
    private fun resolveTransformed(
        value: TransactionValue.Transformed,
        indexedResults: Map<Int, Any?>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Any? {
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
                args = arrayOf(e.message ?: e.toString()),
                cause = e
            )
        }
    }

    private fun resolveFromStep(
        value: TransactionValue.FromStep,
        indexedResults: Map<Int, Any?>,
        handleToIndexMap: Map<StepHandle<*>, Int>
    ): Any? {
        val stepIndex = handleToIndexMap[value.handle]!! // Walidowane wcześniej
        // Logika pętli `execute` gwarantuje, że jeśli dotarliśmy do tego miejsca,
        // to krok `stepIndex` został wykonany, a jego wynik znajduje się w mapie.
        val sourceResult = indexedResults[stepIndex] ?: return null // Wynik kroku może być null

        return when (value) {
            is TransactionValue.FromStep.Field -> resolveField(value, sourceResult, stepIndex)
            is TransactionValue.FromStep.Column -> resolveColumn(value, sourceResult, stepIndex)
            is TransactionValue.FromStep.Row -> sourceResult.toRowMap(value.rowIndex, stepIndex)
        }
    }

    private fun resolveField(
        value: TransactionValue.FromStep.Field,
        sourceResult: Any, // Wiemy, że nie jest null z poprzedniego kroku
        stepIndex: Int
    ): Any? {
        val rowData = sourceResult.toRowMap(value.rowIndex, stepIndex)
        val colName = value.columnName ?: SCALAR_RESULT_KEY

        if (!rowData.containsKey(colName)) {
            val errorMsg = if (value.columnName == null) {
                StepDependencyExceptionMessage.SCALAR_NOT_FOUND
            } else {
                StepDependencyExceptionMessage.COLUMN_NOT_FOUND
            }
            throw StepDependencyException(errorMsg, stepIndex, colName)
        }
        return rowData[colName]
    }

    private fun resolveColumn(
        value: TransactionValue.FromStep.Column,
        sourceResult: Any, // Wiemy, że nie jest null
        stepIndex: Int
    ): List<Any?> {
        // toList, toColumn, toListOf
        val sourceList = sourceResult as? List<*>
            ?: throw StepDependencyException(StepDependencyExceptionMessage.RESULT_NOT_LIST, stepIndex)

        if (value.columnName == null) {
            return sourceList
        }

        val columnName = value.columnName!!

        return sourceList.map { element ->
            val row = element as? Map<*, *>
                ?: throw StepDependencyException(StepDependencyExceptionMessage.RESULT_NOT_MAP_LIST, stepIndex)

            // Sprawdzamy, czy klucz istnieje.
            if (!row.containsKey(columnName)) {
                throw StepDependencyException(StepDependencyExceptionMessage.COLUMN_NOT_FOUND, stepIndex, columnName)
            }

            // Bezpiecznie pobieramy wartość.
            row[columnName]
        }
    }

    @Suppress("UNCHECKED_CAST")
    // Sygnatura `toRowMap` przyjmuje `Any`, bo `null` jest obsługiwany wcześniej w `resolveReference`.
    private fun Any.toRowMap(rowIndex: Int, stepIndex: Int): Map<String, Any?> {
        return when (this) {
            // toList, toColumn, toListOf
            is List<*> -> {
                if (rowIndex >= this.size) {
                    throw StepDependencyException(StepDependencyExceptionMessage.ROW_INDEX_OUT_OF_BOUNDS, stepIndex, rowIndex, this.size)
                }
                when (val element = this[rowIndex]) {
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
                this as Map<String, Any?>
            }
            // toSingleOf, toField, execute
            else -> {
                if (rowIndex > 0) {
                    throw StepDependencyException(StepDependencyExceptionMessage.INVALID_ROW_ACCESS_ON_NON_LIST, stepIndex, rowIndex)
                }
                // Wynik toSingleOf (błąd w konwerterze i type registry gdy użyte na niekompozycie)
                // toField może być nullem, natomiast execute nie
                mapOf(SCALAR_RESULT_KEY to this)
            }
        }
    }
}