package org.octavius.data.contract

/**
 * Kontrakt dla wykonywania operacji CUD (Create, Update, Delete) w atomowych transakcjach.
 */
interface BatchExecutor {
    /**
     * Wykonuje listę kroków bazodanowych w pojedynczej, atomowej transakcji.
     *
     * @param databaseSteps Lista operacji (Insert, Update, Delete, RawSql) do wykonania.
     * @return Mapa, gdzie kluczem jest indeks operacji, a wartością lista zwróconych wierszy.
     * @throws Exception w przypadku niepowodzenia (transakcja jest wycofywana).
     */
    fun execute(databaseSteps: List<DatabaseStep>): Map<Int, List<Map<String, Any?>>>
}