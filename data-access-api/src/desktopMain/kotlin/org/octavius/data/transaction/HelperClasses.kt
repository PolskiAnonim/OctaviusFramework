package org.octavius.data.transaction

import org.octavius.data.DataResult
import org.octavius.data.builder.QueryBuilder

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
         * @param asTypedArray Jeśli `true`, wynik zostanie przekształcony w tablicę typowaną
         *                     (np. `IntArray`, `Array<String>`). Jest to kluczowa optymalizacja
         *                     wydajności dla masowego przekazywania typów prostych, ponieważ
         *                     sterownik JDBC może wysłać je jako pojedynczy, binarny parametr.
         *                     Domyślnie `false` (wynikiem jest `List<Any?>`).
         */
        data class Column(
            override val handle: StepHandle<*>,
            val columnName: String?,
            val asTypedArray: Boolean = false
        ) : FromStep(handle) {
            constructor(handle: StepHandle<*>, asTypedArray: Boolean) : this(handle, null, asTypedArray)
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
}

class TransactionStep<T>(
    // Wszystkie pola muszą być publiczne, aby Executor miał do nich dostęp
    val builder: QueryBuilder<*>,
    val executionLogic: (builder: QueryBuilder<*>, params: Map<String, Any?>) -> DataResult<T>,
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
