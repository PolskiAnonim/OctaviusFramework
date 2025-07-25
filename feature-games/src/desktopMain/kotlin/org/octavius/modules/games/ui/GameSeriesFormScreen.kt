package org.octavius.modules.games.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations
import org.octavius.modules.games.form.series.GameSeriesFormDataManager
import org.octavius.modules.games.form.series.GameSeriesFormSchemaBuilder
import org.octavius.modules.games.form.series.GameSeriesFormValidator

class GameSeriesFormScreen {

    companion object {
        fun create(
            entityId: Int? = null, onSaveSuccess: () -> Unit = {}, onCancel: () -> Unit = {}
        ): FormScreen {
            val title =
                if (entityId == null) Translations.get("games.form.newSeries") else Translations.get("games.form.editSeries")

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = GameSeriesFormSchemaBuilder(),
                formDataManager = GameSeriesFormDataManager(),
                formValidator = GameSeriesFormValidator()
            )
            return FormScreen(title, formHandler, onSaveSuccess, onCancel)
        }
    }
}