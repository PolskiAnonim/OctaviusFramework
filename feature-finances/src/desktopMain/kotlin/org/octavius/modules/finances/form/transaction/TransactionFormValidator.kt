package org.octavius.modules.finances.form.transaction

import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrentAs
import org.octavius.form.control.type.repeatable.RepeatableResultValue
import org.octavius.localization.Tr
import java.math.BigDecimal

class TransactionFormValidator : FormValidator() {

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> {
        return mapOf("save" to { formData -> validateTransactionBalance(formData) })
    }

    private fun validateTransactionBalance(formResultData: FormResultData): Boolean {
        val splits = formResultData.getCurrentAs<RepeatableResultValue>("splits")
        
        var total = BigDecimal.ZERO
        splits.allCurrentRows.forEach { rowData ->
            val amount = rowData.getCurrentAs<BigDecimal>("amount")
            total = total.add(amount)
        }

        return if (total.compareTo(BigDecimal.ZERO) != 0) {
            errorManager.setFieldErrors("splits", listOf(Tr.Finances.Transaction.notBalanced()))
            false
        } else {
            true
        }
    }
}
