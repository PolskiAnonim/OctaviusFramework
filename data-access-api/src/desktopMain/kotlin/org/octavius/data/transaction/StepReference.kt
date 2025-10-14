// org/octavius/data/transaction/StepReference.kt

package org.octavius.data.transaction

/**
 * Reprezentuje "uchwyt" do przyszłego wyniku kroku transakcyjnego.
 * Umożliwia tworzenie referencji do różnych części wyniku tego kroku,
 * które mogą być użyte jako parametry w kolejnych krokach.
 */
class StepReference internal constructor(private val index: Int) {

    /**
     * Tworzy referencję do wartości w pojedynczej komórce (`wiersz`, `kolumna`).
     * Domyślnie pobiera wartość z pierwszej kolumny (`rowIndex = 0`).
     *
     * Zastosowanie: Pobranie ID nowo wstawionego rekordu.
     * `val newUserId = plan.insert(...).field("id")`
     *
     * @param columnName Nazwa kolumny do pobrania.
     * @param rowIndex Indeks wiersza (0-based).
     * @return Instancja [TransactionValue.FromStep.Field].
     */
    fun field(columnName: String, rowIndex: Int = 0): TransactionValue.FromStep.Field {
        return TransactionValue.FromStep.Field(index, columnName, rowIndex)
    }

    /**
     * Tworzy referencję do wszystkich wartości w danej kolumnie (jako lista).
     *
     * Zastosowanie: Pobranie listy ID do użycia w klauzuli `ANY/ALL`.
     * `val userIds = plan.select(...).column("id")`
     * `plan.delete(..., filter = mapOf("id" to userIds))`
     *
     * @param columnName Nazwa kolumny.
     * @param asTypedArray Jeśli `true`, wynik zostanie przekształcony w tablicę typowaną
     *                     (np. `IntArray`, `Array<String>`). Jest to kluczowa optymalizacja
     *                     wydajności dla masowego przekazywania typów prostych, ponieważ
     *                     sterownik JDBC może wysłać je jako pojedynczy, binarny parametr.
     *                     Domyślnie `false` (wynikiem jest `List<Any?>`).
     * @return Instancja [TransactionValue.FromStep.Column].
     */
    fun column(columnName: String, asTypedArray: Boolean = false): TransactionValue.FromStep.Column {
        return TransactionValue.FromStep.Column(index, columnName, asTypedArray)
    }

    /**
     * Tworzy referencję do całego wiersza (jako `Map<String, Any?>`).
     * Gdy użyte jako parametr, jego zawartość zostanie "rozsmarowana" i połączona
     * z pozostałymi parametrami kroku.
     *
     * Zastosowanie: Kopiowanie danych z jednego miejsca w drugie.
     * `val userToCopy = plan.select(...).row()`
     * `plan.insert("users_archive", data = mapOf("copy_details" to userToCopy))`
     *
     * @param rowIndex Indeks wiersza (0-based).
     * @return Instancja [TransactionValue.FromStep.Row].
     */
    fun row(rowIndex: Int = 0): TransactionValue.FromStep.Row {
        return TransactionValue.FromStep.Row(index, rowIndex)
    }
}