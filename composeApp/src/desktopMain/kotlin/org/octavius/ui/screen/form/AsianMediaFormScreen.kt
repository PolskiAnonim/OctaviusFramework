package org.octavius.ui.screen.form

import org.octavius.domain.asian.form.AsianMediaFormHandler
import org.octavius.form.component.FormScreen

class AsianMediaFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val formHandler = AsianMediaFormHandler(entityId)
}