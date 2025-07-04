package org.octavius.modules.settings.ui

import org.octavius.localization.Translations
import org.octavius.modules.settings.ApiIntegrationsReportHandler
import org.octavius.navigator.Navigator
import org.octavius.report.component.ReportScreen

class ApiIntegrationsReportScreen(private val navigator: Navigator) : ReportScreen() {
    override val reportHandler = ApiIntegrationsReportHandler(navigator)
    override val title = Translations.get("settings.api.title")
}