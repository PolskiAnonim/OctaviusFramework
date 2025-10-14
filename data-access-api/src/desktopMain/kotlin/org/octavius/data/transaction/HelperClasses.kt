package org.octavius.data.transaction

import org.octavius.data.DataResult

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
    sealed class FromStep(open val stepIndex: Int) : TransactionValue() {
        /**
         * Pobiera pojedynczą wartość z konkretnej komórki (`wiersz`, `kolumna`).
         * Idealne do pobierania ID z właśnie wstawionego wiersza.
         *
         * @param stepIndex Indeks kroku źródłowego.
         * @param columnName Nazwa kolumny, z której ma być pobrana wartość.
         * @param rowIndex Indeks wiersza (domyślnie 0, czyli pierwszy).
         */
        data class Field(
            override val stepIndex: Int,
            val columnName: String,
            val rowIndex: Int = 0
        ) : FromStep(stepIndex)

        /**
         * Pobiera wszystkie wartości z jednej kolumny jako listę lub tablicę typowaną.
         *
         * Używane głównie do przekazywania wyników jednego zapytania jako parametrów
         * dla kolejnego, np. w klauzulach `WHERE id = ANY(:ids)` lub `INSERT ... SELECT ... FROM UNNEST(...)`.
         *
         * @param stepIndex Indeks (0-based) kroku, z którego pochodzą dane.
         * @param columnName Nazwa kolumny, której wartości mają zostać pobrane.
         * @param asTypedArray Jeśli `true`, wynik zostanie przekształcony w tablicę typowaną
         *                     (np. `IntArray`, `Array<String>`). Jest to kluczowa optymalizacja
         *                     wydajności dla masowego przekazywania typów prostych, ponieważ
         *                     sterownik JDBC może wysłać je jako pojedynczy, binarny parametr.
         *                     Domyślnie `false` (wynikiem jest `List<Any?>`).
         */
        data class Column(
            override val stepIndex: Int,
            val columnName: String,
            val asTypedArray: Boolean = false
        ) : FromStep(stepIndex)

        /**
         * Pobiera cały wiersz jako `Map<String, Any?>`.
         * Użyteczne, gdy chcesz przekazać wiele pól z jednego wyniku jako parametry
         * do kolejnego kroku (np. kopiowanie wiersza z modyfikacjami).
         * Executor specjalnie obsługuje ten typ, "rozsmarowując" mapę na parametry.
         *
         * @param stepIndex Indeks kroku źródłowego.
         * @param rowIndex Indeks wiersza (domyślnie 0, czyli pierwszy).
         */
        data class Row(
            override val stepIndex: Int,
            val rowIndex: Int = 0
        ) : FromStep(stepIndex)
    }
}

/**
 * Reprezentuje pojedynczą operację w transakcji bazodanowej.
 */
class TransactionStep<T>(
    val builderState: Any, // Będzie to konkretny builder (DatabaseSelectQueryBuilder, etc.)
    val terminalMethod: (Map<String, Any?>) -> DataResult<T>,
    val params: Map<String, Any?>
)

typealias TransactionPlanResults = Map<Int, List<Map<String, Any?>>>

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
