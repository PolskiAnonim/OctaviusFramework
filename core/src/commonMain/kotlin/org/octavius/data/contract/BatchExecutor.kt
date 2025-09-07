package org.octavius.data.contract

typealias BatchStepResults = Map<Int, List<Map<String, Any?>>>

/**
 * Kontrakt dla wykonywania operacji CUD (Create, Update, Delete) w atomowych transakcjach.
 *
 * Implementacje tego interfejsu powinny zapewniać:
 * - Atomowość: wszystkie operacje kończą się sukcesem lub żadna (rollback)
 * - Obsługę zależności między krokami (DatabaseValue.FromStep)
 * - Obsługę klauzul RETURNING dla pobrania wygenerowanych wartości
 *
 * @see DatabaseStep
 * @see DatabaseValue
 */
interface BatchExecutor {
    /**
     * Wykonuje listę kroków bazodanowych w pojedynczej, atomowej transakcji.
     *
     * Kroki są wykonywane sekwencyjnie w podanej kolejności.
     * Wyniki z wcześniejszych kroków mogą być używane w późniejszych przez DatabaseValue.FromStep.
     * W przypadku błędu w dowolnym kroku, cała transakcja zostaje wycofana.
     *
     * @param databaseSteps Lista operacji (Insert, Update, Delete, RawSql) do wykonania.
     * @return DataResult zawierający mapę, gdzie kluczem jest indeks operacji (0-based),
     *         a wartością lista zwróconych wierszy lub informacji o liczbie zmian.
     */
    fun execute(databaseSteps: List<DatabaseStep>): DataResult<BatchStepResults>
}
