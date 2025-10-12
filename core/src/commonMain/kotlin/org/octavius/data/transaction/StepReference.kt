package org.octavius.data.transaction

// Ta klasa to "uchwyt" do przyszłego wyniku kroku.
// Jest wewnętrznie zarządzana przez TransactionPlan.
class StepReference internal constructor(private val index: Int) {
    fun get(columnName: String): TransactionValue {
        return TransactionValue.FromStep(index, columnName)
    }
}