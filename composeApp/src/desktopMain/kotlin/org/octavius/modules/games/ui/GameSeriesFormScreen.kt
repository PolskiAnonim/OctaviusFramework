package org.octavius.modules.games.ui

import org.octavius.form.component.FormScreen
import org.octavius.localization.Translations
import org.octavius.modules.games.form.series.GameSeriesFormHandler

class GameSeriesFormScreen(
    entityId: Int? = null,
    onSaveSuccess: () -> Unit = {},
    onCancel: () -> Unit = {}
) : FormScreen(onSaveSuccess, onCancel) {
    override val title = if (entityId == null) Translations.get("games.form.newSeries") else Translations.get("games.form.editSeries")
    override val formHandler = GameSeriesFormHandler(entityId)
}