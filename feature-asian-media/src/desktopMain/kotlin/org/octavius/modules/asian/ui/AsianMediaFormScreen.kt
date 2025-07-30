package org.octavius.modules.asian.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations
import org.octavius.modules.asian.form.AsianMediaFormDataManager
import org.octavius.modules.asian.form.AsianMediaFormSchemaBuilder
import org.octavius.modules.asian.form.AsianMediaValidator

class AsianMediaFormScreen {
    companion object {
        fun create(
            entityId: Int? = null,
            payload: Map<String, Any?>? = null,
            onSaveSuccess: () -> Unit,
            onCancel: () -> Unit = {}
        ): FormScreen {
            val title =
                if (entityId == null) Translations.get("asianMedia.form.newTitle") else Translations.get("asianMedia.form.editTitle")

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = AsianMediaFormSchemaBuilder(),
                formDataManager = AsianMediaFormDataManager(),
                formValidator = AsianMediaValidator(entityId),
                payload = payload
            )

            return FormScreen(title, formHandler, onSaveSuccess, onCancel)
        }
    }
}