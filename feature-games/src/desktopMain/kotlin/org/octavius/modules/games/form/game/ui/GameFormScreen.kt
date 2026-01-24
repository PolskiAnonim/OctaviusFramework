package org.octavius.modules.games.form.game.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.modules.games.form.game.GameFormDataManager
import org.octavius.modules.games.form.game.GameFormSchemaBuilder
import org.octavius.modules.games.form.game.GameFormValidator

class GameFormScreen {
    companion object {
        fun create(
            entityId: Int? = null,
            payload: Map<String, Any?> = emptyMap()
        ): FormScreen {
            val title =
                if (entityId == null) Tr.Games.Form.newGame() else Tr.Games.Form.editGame()

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = GameFormSchemaBuilder(),
                formDataManager = GameFormDataManager(),
                formValidator = GameFormValidator(entityId),
                payload = payload
            )

            return FormScreen(title, formHandler)
        }
    }

}