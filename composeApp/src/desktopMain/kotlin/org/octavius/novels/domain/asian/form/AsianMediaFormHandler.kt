package org.octavius.novels.domain.asian.form

import org.octavius.novels.form.component.FormDataManager
import org.octavius.novels.form.component.FormHandler
import org.octavius.novels.form.component.FormSchema


class AsianMediaFormHandler(entityId: Int?) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema {
        return AsianMediaFormSchemaBuilder().build()
    }

    override fun createDataManager(): FormDataManager {
        return AsianMediaFormDataManager()
    }
}