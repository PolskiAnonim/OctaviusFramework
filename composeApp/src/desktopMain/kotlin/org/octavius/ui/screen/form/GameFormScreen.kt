package org.octavius.ui.screen.form

import org.octavius.domain.game.form.GameFormHandler
import org.octavius.form.component.FormScreen

class GameFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val formHandler = GameFormHandler(entityId)
}