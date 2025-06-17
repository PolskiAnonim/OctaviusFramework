package org.octavius.ui.screen.form

import org.octavius.domain.game.form.GameFormHandler
import org.octavius.form.component.FormScreen

class GameFormScreen(entityId: Int? = null) : FormScreen() {
    override val formHandler = GameFormHandler(entityId)
}