package org.octavius.modules.asian.form

import org.octavius.form.component.FormDataManager
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormValidator


class AsianMediaFormHandler(entityId: Int?) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema {
        return AsianMediaFormSchemaBuilder().build()
    }

    override fun createDataManager(): FormDataManager {
        return AsianMediaFormDataManager()
    }

    override fun createFormValidator(): FormValidator {
        return AsianMediaValidator(entityId)
    }
}