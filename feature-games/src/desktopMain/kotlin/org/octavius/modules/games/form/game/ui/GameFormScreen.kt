package org.octavius.modules.games.form.game.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations
import org.octavius.modules.games.form.game.GameFormDataManager
import org.octavius.modules.games.form.game.GameFormSchemaBuilder

class GameFormScreen {
    companion object {
        fun create(
            entityId: Int? = null,
            payload: Map<String, Any?>? = null
        ): FormScreen {
            val title =
                if (entityId == null) Translations.get("games.form.newGame") else Translations.get("games.form.editGame")

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = GameFormSchemaBuilder(),
                formDataManager = GameFormDataManager(),
                payload = payload
            )

            return FormScreen(title, formHandler)
        }
    }

}