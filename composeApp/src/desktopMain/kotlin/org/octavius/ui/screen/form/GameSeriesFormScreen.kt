package org.octavius.ui.screen.form

import org.octavius.domain.game.form.GameSeriesFormHandler
import org.octavius.form.component.FormScreen

class GameSeriesFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val formHandler = GameSeriesFormHandler(entityId)
}