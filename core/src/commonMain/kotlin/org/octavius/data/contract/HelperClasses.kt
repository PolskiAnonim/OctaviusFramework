package org.octavius.data.contract

/**
 * Reprezentuje wartość w kroku bazodanowym.
 *
 * Umożliwia przekazywanie zarówno stałych wartości, jak i dynamicznych referencji
 * do wyników poprzednich kroków w tej samej transakcji.
 *
 * @see TransactionStep
 */
sealed class DatabaseValue {
    /**
     * Stała, predefiniowana wartość.
     * @param value Wartość do użycia w operacji.
     */
    data class Value(val value: Any?) : DatabaseValue()

    /**
     * Referencja do wyniku z poprzedniego kroku w tej samej transakcji.
     *
     * @param stepIndex Indeks (0-based) kroku, którego wynik ma być użyty.
     * @param resultKey Nazwa kolumny (np. "id") w wyniku tego kroku.
     */
    data class FromStep(val stepIndex: Int, val resultKey: String) : DatabaseValue()
}

/**
 * Reprezentuje pojedynczą operację w transakcji bazodanowej.
 *
 * Każda operacja może opcjonalnie zwracać wartości przez klauzulę `RETURNING`.
 *
 * @see BatchExecutor.execute
 */
sealed class TransactionStep {


    /** Operacja `INSERT`. */
    data class Insert(
        val tableName: String,
        val data: Map<String, DatabaseValue>,
        /** Lista kolumn do zwrócenia przez `RETURNING`. Pusta lista oznacza brak klauzuli. */
        val returning: List<String> = listOf()
    ) : TransactionStep()

    /** Operacja `UPDATE`. */
    data class Update(
        val tableName: String,
        val data: Map<String, DatabaseValue>,
        val filter: Map<String, DatabaseValue>,
        /** Lista kolumn do zwrócenia przez `RETURNING`. Pusta lista oznacza brak klauzuli. */
        val returning: List<String> = emptyList()
    ) : TransactionStep()

    /** Operacja `DELETE`. */
    data class Delete(
        val tableName: String,
        val filter: Map<String, DatabaseValue>,
        /** Lista kolumn do zwrócenia przez `RETURNING`. Pusta lista oznacza brak klauzuli. */
        val returning: List<String> = emptyList()
    ) : TransactionStep()

    /**
     * Krok bazodanowy stworzony za pomocą buildera używając asStep() z metodą terminalną
     *
     * @param builderState Pełny stan buildera (wszystkie klauzule, filtry, etc.)
     * @param terminalMethod Referencja do metody terminalnej do wywołania
     * @param params Parametry do przekazania metodzie terminalnej
     */
    data class FromBuilder<T>(
        val builderState: Any, // Będzie to konkretny builder (DatabaseSelectQueryBuilder, etc.)
        val terminalMethod: (Map<String, Any?>) -> DataResult<T>,
        val params: Map<String, Any?>
    ) : TransactionStep()
}

/**
 * Opakowuje wartość, aby jawnie określić docelowy typ PostgreSQL.
 *
 * Powoduje dodanie rzutowania typu (`::pgType`) do wygenerowanego fragmentu SQL.
 * Przydatne do obsługi niejednoznaczności typów, np. przy tablicach.
 *
 * @param value Wartość do osadzenia w zapytaniu (należy unikać data class gdzie jest to dodawane automatycznie!).
 * @param pgType Nazwa typu PostgreSQL, na który wartość ma być rzutowana (np. "text[]", "jsonb").
 */
data class PgTyped(val value: Any?, val pgType: String)


/**
 * Identyfikuje kolumnę w bazie danych, uwzględniając nazwę tabeli.
 *
 * Używane do rozróżniania kolumn o tych samych nazwach w zapytaniach z `JOIN`.
 *
 * @param tableName Nazwa tabeli źródłowej.
 * @param fieldName Nazwa kolumny.
 */
data class ColumnInfo(val tableName: String, val fieldName: String)

/**
 * Helper function do tworzenia referencji do wyników poprzednich kroków w transakcji.
 *
 * @param stepIndex Indeks kroku (0-based) z którego chcemy pobrać wynik
 * @param resultKey Nazwa kolumny/klucza z wyniku tego kroku
 */
fun stepResult(stepIndex: Int, resultKey: String): DatabaseValue.FromStep {
    return DatabaseValue.FromStep(stepIndex, resultKey)
}
