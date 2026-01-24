package org.octavius.modules.asian.form.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.modules.asian.form.AsianMediaFormDataManager
import org.octavius.modules.asian.form.AsianMediaFormSchemaBuilder
import org.octavius.modules.asian.form.AsianMediaValidator

class AsianMediaFormScreen {
    companion object {
        fun create(
            entityId: Int? = null,
            payload: Map<String, Any?> = emptyMap()
        ): FormScreen {
            val title =
                if (entityId == null) Tr.AsianMedia.Form.newTitle() else Tr.AsianMedia.Form.editTitle()

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = AsianMediaFormSchemaBuilder(),
                formDataManager = AsianMediaFormDataManager(),
                formValidator = AsianMediaValidator(entityId),
                payload = payload
            )

            return FormScreen(title, formHandler)
        }
    }
}