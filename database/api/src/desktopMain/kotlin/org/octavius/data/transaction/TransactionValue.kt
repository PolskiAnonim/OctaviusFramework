package org.octavius.data.transaction

/**
 * Reprezentuje wartość w kroku transakcyjnym.
 * Umożliwia przekazywanie zarówno stałych wartości, jak i dynamicznych referencji
 * do wyników poprzednich kroków w tej samej transakcji.
 */
sealed class TransactionValue {
    /**
     * Stała, predefiniowana wartość.
     * @param value Wartość do użycia w operacji.
     */
    data class Value(val value: Any?) : TransactionValue()

    /**
     * Referencja do wyniku z poprzedniego kroku. Ta klasa jest bazą dla
     * bardziej specyficznych typów referencji.
     */
    sealed class FromStep(open val handle: StepHandle<*>) : TransactionValue() {
        /**
         * Pobiera pojedynczą wartość z konkretnej komórki (`wiersz`, `kolumna`).
         * Idealne do pobierania ID z właśnie wstawionego wiersza.
         *
         * @param handle Uchwyt do kroku, z którego pochodzą dane.
         * @param columnName Nazwa kolumny, z której ma być pobrana wartość.
         * @param rowIndex Indeks wiersza (domyślnie 0, czyli pierwszy).
         */
        data class Field(
            override val handle: StepHandle<*>,
            val columnName: String?,
            val rowIndex: Int = 0
        ) : FromStep(handle) {
            constructor(handle: StepHandle<*>, rowIndex: Int = 0) : this(handle, null, rowIndex)
        }

        /**
         * Pobiera wszystkie wartości z jednej kolumny jako listę lub tablicę typowaną.
         *
         * Używane głównie do przekazywania wyników jednego zapytania jako parametrów
         * dla kolejnego, np. w klauzulach `WHERE id = ANY(:ids)` lub `INSERT ... SELECT ... FROM UNNEST(...)`.
         *
         * @param handle Uchwyt do kroku, z którego pochodzą dane.
         * @param columnName Nazwa kolumny, której wartości mają zostać pobrane.
         */
        data class Column(
            override val handle: StepHandle<*>,
            val columnName: String?
        ) : FromStep(handle) {
            constructor(handle: StepHandle<*>) : this(handle, null)
        }

        /**
         * Pobiera cały wiersz jako `Map<String, Any?>`.
         * Użyteczne, gdy chcesz przekazać wiele pól z jednego wyniku jako parametry
         * do kolejnego kroku (np. kopiowanie wiersza z modyfikacjami).
         * Executor specjalnie obsługuje ten typ, "rozsmarowując" mapę na parametry.
         *
         * @param handle Uchwyt do kroku, z którego pochodzą dane.
         * @param rowIndex Indeks wiersza (domyślnie 0, czyli pierwszy).
         */
        data class Row(
            override val handle: StepHandle<*>,
            val rowIndex: Int = 0
        ) : FromStep(handle)
    }

    /**
     * Wynik transformacji innej wartości.
     */
    class Transformed(
        val source: TransactionValue,
        val transform: (Any?) -> Any?
    ) : TransactionValue()
}

fun TransactionValue.map(transformation: (Any?) -> Any?): TransactionValue {
    return TransactionValue.Transformed(this, transformation)
}

/**
 * Konwertuje dowolną wartość (także null) na instancję [TransactionValue.Value].
 *
 * Stanowi zwięzłą alternatywę dla jawnego wywołania konstruktora,
 * poprawiając czytelność operacji budujących kroki transakcji.
 *
 * Przykład użycia:
 * `val idRef = 123.toTransactionValue()` zamiast `val idRef = TransactionValue.Value(123)`
 *
 * @return Instancja [TransactionValue.Value] opakowująca tę wartość.
 * @see TransactionValue
 */
fun Any?.toTransactionValue(): TransactionValue = TransactionValue.Value(this)
