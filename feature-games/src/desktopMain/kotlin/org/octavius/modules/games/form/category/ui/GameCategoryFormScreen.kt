package org.octavius.modules.games.form.category.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.T
import org.octavius.modules.games.form.category.GameCategoryDataManager
import org.octavius.modules.games.form.category.GameCategorySchemaBuilder
import org.octavius.modules.games.form.category.GameCategoryValidator

class GameCategoryFormScreen {

    companion object {
        fun create(
            entityId: Int? = null
        ): FormScreen {
            val title =
                if (entityId == null) T.get("games.form.newSeries") else T.get("games.form.editSeries")

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = GameCategorySchemaBuilder(),
                formDataManager = GameCategoryDataManager(),
                formValidator = GameCategoryValidator(),
            )
            return FormScreen(title, formHandler)
        }
    }
}