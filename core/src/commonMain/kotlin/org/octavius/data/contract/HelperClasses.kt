package org.octavius.data.contract

/**
 * Reprezentuje wartość w kroku bazodanowym.
 *
 * Umożliwia przekazywanie zarówno stałych wartości, jak i dynamicznych referencji
 * do wyników poprzednich kroków w tej samej transakcji.
 *
 * @see DatabaseStep
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
 * Konwertuje dowolną  wartość (także null) na instancję [DatabaseValue.Value].
 *
 * Stanowi zwięzłą alternatywę dla jawnego wywołania konstruktora,
 * poprawiając czytelność operacji budujących kroki transakcji.
 *
 * Przykład użycia:
 * `val idRef = 123.toDatabaseValue()` zamiast `val idRef = DatabaseValue.Value(123)`
 *
 * @return Instancja [DatabaseValue.Value] opakowująca tę wartość.
 * @see DatabaseValue
 */
fun Any?.toDatabaseValue(): DatabaseValue {
    return DatabaseValue.Value(this)
}

/**
 * Reprezentuje pojedynczą operację w transakcji bazodanowej.
 *
 * Każda operacja może opcjonalnie zwracać wartości przez klauzulę `RETURNING`.
 *
 * @see BatchExecutor.execute
 */
sealed class DatabaseStep {
    /** Lista kolumn do zwrócenia przez `RETURNING`. Pusta lista oznacza brak klauzuli. */
    abstract val returning: List<String>

    /** Operacja `INSERT`. */
    data class Insert(
        val tableName: String,
        val data: Map<String, DatabaseValue>,
        override val returning: List<String> = listOf()
    ) : DatabaseStep()

    /** Operacja `UPDATE`. */
    data class Update(
        val tableName: String,
        val data: Map<String, DatabaseValue>,
        val filter: Map<String, DatabaseValue>,
        override val returning: List<String> = emptyList()
    ) : DatabaseStep()

    /** Operacja `DELETE`. */
    data class Delete(
        val tableName: String,
        val filter: Map<String, DatabaseValue>,
        override val returning: List<String> = emptyList()
    ) : DatabaseStep()

    /** Dowolna operacja SQL z nazwanymi parametrami.
     * Umożliwia wykonywanie złożonych zapytań, które nie mieszczą się
     * w standardowych operacjach INSERT/UPDATE/DELETE.
     */
    data class RawSql(
        val sql: String,
        val params: Map<String, DatabaseValue>,
        override val returning: List<String> = emptyList()
    ) : DatabaseStep()
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
