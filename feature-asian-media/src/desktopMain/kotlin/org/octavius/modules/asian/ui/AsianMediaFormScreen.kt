package org.octavius.modules.asian.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations
import org.octavius.modules.asian.form.AsianMediaFormDataManager
import org.octavius.modules.asian.form.AsianMediaFormSchemaBuilder
import org.octavius.modules.asian.form.AsianMediaValidator

class AsianMediaFormScreen(
    entityId: Int? = null, onSaveSuccess: () -> Unit = {}, onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val title =
        if (entityId == null) Translations.get("asianMedia.form.newTitle") else Translations.get("asianMedia.form.editTitle")

    override val formHandler = FormHandler(
        entityId = entityId,
        formSchemaBuilder = AsianMediaFormSchemaBuilder(),
        formDataManager = AsianMediaFormDataManager(),
        formValidator = AsianMediaValidator(entityId)
    )
}