package org.octavius.modules.games.form.category.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.modules.games.form.category.GameCategoryDataManager
import org.octavius.modules.games.form.category.GameCategorySchemaBuilder
import org.octavius.modules.games.form.category.GameCategoryValidator

class GameCategoryFormScreen {

    companion object {
        fun create(
            entityId: Int? = null
        ): FormScreen {
            val title =
                if (entityId == null) Tr.Games.Form.newCategory() else Tr.Games.Form.editCategory()

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