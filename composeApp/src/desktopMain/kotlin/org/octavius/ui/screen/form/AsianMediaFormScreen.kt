package org.octavius.ui.screen.form

import org.octavius.asianMedia.form.AsianMediaFormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations

class AsianMediaFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val title = if (entityId == null) Translations.get("asianMedia.form.newTitle") else Translations.get("asianMedia.form.editTitle")
    override val formHandler = AsianMediaFormHandler(entityId)
}