package org.octavius.modules.games.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations
import org.octavius.modules.games.form.game.GameFormDataManager
import org.octavius.modules.games.form.game.GameFormSchemaBuilder

class GameFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val title =
        if (entityId == null) Translations.get("games.form.newGame") else Translations.get("games.form.editGame")

    override val formHandler = FormHandler(
        entityId = entityId,
        formSchemaBuilder = GameFormSchemaBuilder(),
        formDataManager = GameFormDataManager())
}