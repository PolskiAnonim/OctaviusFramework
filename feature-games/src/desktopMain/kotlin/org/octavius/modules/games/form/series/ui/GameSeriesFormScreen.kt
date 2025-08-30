package org.octavius.modules.games.form.series.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.T
import org.octavius.modules.games.form.series.GameSeriesFormDataManager
import org.octavius.modules.games.form.series.GameSeriesFormSchemaBuilder
import org.octavius.modules.games.form.series.GameSeriesFormValidator

class GameSeriesFormScreen {

    companion object {
        fun create(
            entityId: Int? = null
        ): FormScreen {
            val title =
                if (entityId == null) T.get("games.form.newSeries") else T.get("games.form.editSeries")

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = GameSeriesFormSchemaBuilder(),
                formDataManager = GameSeriesFormDataManager(),
                formValidator = GameSeriesFormValidator(entityId),
            )
            return FormScreen(title, formHandler)
        }
    }
}