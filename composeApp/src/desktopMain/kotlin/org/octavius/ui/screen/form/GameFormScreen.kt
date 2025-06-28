package org.octavius.ui.screen.form

import org.octavius.games.form.GameFormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations

class GameFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val title = if (entityId == null) Translations.get("games.form.newGame") else Translations.get("games.form.editGame")
    override val formHandler = GameFormHandler(entityId)
}