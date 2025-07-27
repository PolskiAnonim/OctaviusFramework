package org.octavius.data.contract

/**
 * Interfejs definiujący kontrakt dla wykonywania atomowych transakcji bazodanowych.
 *
 * Abstrakcja nad mechanizmem wykonywania operacji CUD (Create, Update, Delete),
 * pozwalająca na grupowanie wielu kroków w jedną, spójną transakcję.
 */
interface TransactionManager {
    /**
     * Wykonuje listę kroków bazodanowych w pojedynczej, atomowej transakcji.
     *
     * @param databaseSteps Lista operacji (Insert, Update, Delete, RawSql) do wykonania.
     * @return Mapa, gdzie kluczem jest indeks operacji, a wartością lista zwróconych wierszy (z klauzuli RETURNING).
     * @throws Exception w przypadku niepowodzenia którejkolwiek operacji (transakcja jest wycofywana).
     */
    fun execute(databaseSteps: List<DatabaseStep>): Map<Int, List<Map<String, Any?>>>
}