package org.octavius.data.transaction

import java.util.*

/**
 * Bezpieczny typowo, unikalny identyfikator dla kroku w transakcji.
 */
class StepHandle<T> internal constructor() {
    private val id: UUID = UUID.randomUUID()

    /**
     * Tworzy referencję do wartości skalarnej, gdy krok zwraca pojedynczą wartość
     * (np. z `toField()` lub `execute()`).
     *
     * @param rowIndex Indeks wiersza (zazwyczaj 0).
     */
    fun field(rowIndex: Int = 0): TransactionValue.FromStep.Field {
        return TransactionValue.FromStep.Field(this, rowIndex)
    }

    /**
     * Tworzy referencję do wartości w konkretnej kolumnie, gdy krok zwraca wiersz(e)
     * (np. z `toList()` lub `toSingle()`).
     *
     * @param columnName Nazwa kolumny do pobrania.
     * @param rowIndex Indeks wiersza (zazwyczaj 0).
     */
    fun field(columnName: String, rowIndex: Int = 0): TransactionValue.FromStep.Field {
        return TransactionValue.FromStep.Field(this, columnName, rowIndex)
    }

    /** Pobiera całą kolumnę z wyniku, który jest listą skalarów (wynik `toColumn()`). */
    fun column(): TransactionValue.FromStep.Column {
        return TransactionValue.FromStep.Column(this)
    }

    /** Pobiera wartości z podanej kolumny z wyniku, który jest listą wierszy (wynik `toList()`). */
    fun column(columnName: String): TransactionValue.FromStep.Column {
        return TransactionValue.FromStep.Column(this, columnName)
    }

    fun row(rowIndex: Int = 0): TransactionValue.FromStep.Row {
        return TransactionValue.FromStep.Row(this, rowIndex)
    }

    override fun equals(other: Any?) = other is StepHandle<*> && id == other.id
    override fun hashCode() = id.hashCode()
    override fun toString(): String = "StepHandle(id=$id)"
}