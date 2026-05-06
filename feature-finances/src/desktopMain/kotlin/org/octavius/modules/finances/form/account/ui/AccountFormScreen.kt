package org.octavius.modules.finances.form.account.ui

import org.octavius.modules.finances.form.account.AccountFormDataManager
import org.octavius.modules.finances.form.account.AccountFormSchemaBuilder
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr

class AccountFormScreen {
    companion object {
        fun create(
            entityId: Int? = null
        ): FormScreen {
            val title = if (entityId == null) Tr.Finances.Account.new() else Tr.Finances.Account.edit()

            return FormScreen(
                title,
                FormHandler(
                    formSchemaBuilder = AccountFormSchemaBuilder(),
                    formDataManager = AccountFormDataManager(),
                    payload = mapOf("id" to entityId)
                )
            )
        }
    }
}
