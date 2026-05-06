package org.octavius.modules.finances.form.transaction.ui

import org.octavius.modules.finances.form.transaction.TransactionFormDataManager
import org.octavius.modules.finances.form.transaction.TransactionFormSchemaBuilder
import org.octavius.modules.finances.form.transaction.TransactionFormValidator
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr

class TransactionFormScreen {
    companion object {
        fun create(
            entityId: Long? = null
        ): FormScreen {
            val title = if (entityId == null) Tr.Finances.Transaction.new() else Tr.Finances.Transaction.edit()

            return FormScreen(
                title,
                FormHandler(
                    formSchemaBuilder = TransactionFormSchemaBuilder(),
                    formDataManager = TransactionFormDataManager(),
                    formValidator = TransactionFormValidator(),
                    payload = mapOf("id" to entityId)
                )
            )
        }
    }
}
