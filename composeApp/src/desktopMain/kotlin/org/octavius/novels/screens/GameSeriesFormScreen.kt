package org.octavius.novels.screens

import org.octavius.novels.domain.game.form.GameSeriesFormHandler
import org.octavius.novels.form.component.FormScreen

class GameSeriesFormScreen(entityId: Int? = null) : FormScreen() {
    override val formHandler = GameSeriesFormHandler(entityId)
}