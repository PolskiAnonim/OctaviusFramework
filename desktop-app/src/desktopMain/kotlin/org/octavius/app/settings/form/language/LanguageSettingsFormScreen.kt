package org.octavius.app.settings.form.language

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.app.settings.AppSettingsManager

class LanguageSettingsFormScreen {
    companion object {
        fun create(settingsManager: AppSettingsManager): FormScreen {
            val title = Tr.Settings.Language.title()
            val formHandler = FormHandler(
                formSchemaBuilder = LanguageSettingsSchemaBuilder(),
                formDataManager = LanguageSettingsDataManager(settingsManager)
            )
            return FormScreen(title, formHandler)
        }
    }
}
