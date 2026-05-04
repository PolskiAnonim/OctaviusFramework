package org.octavius.app.settings.form.database

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.app.settings.AppSettingsManager

class DatabaseSettingsFormScreen {
    companion object {
        fun create(settingsManager: AppSettingsManager): FormScreen {
            val title = Tr.Settings.Database.title()
            val formHandler = FormHandler(
                formSchemaBuilder = DatabaseSettingsSchemaBuilder(),
                formDataManager = DatabaseSettingsDataManager(settingsManager)
            )
            return FormScreen(title, formHandler)
        }
    }
}
