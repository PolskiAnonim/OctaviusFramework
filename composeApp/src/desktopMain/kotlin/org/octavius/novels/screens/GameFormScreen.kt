package org.octavius.novels.screens

import org.octavius.novels.domain.game.form.GameFormHandler
import org.octavius.novels.form.component.FormScreen

class GameFormScreen(entityId: Int? = null) : FormScreen() {
    override val formHandler = GameFormHandler(entityId)
}