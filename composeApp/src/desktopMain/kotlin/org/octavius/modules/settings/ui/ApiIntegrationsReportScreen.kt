package org.octavius.modules.settings.ui

import org.octavius.localization.Translations
import org.octavius.modules.settings.ApiIntegrationsReportStructureBuilder
import org.octavius.navigator.Navigator
import org.octavius.report.component.ReportScreen
import org.octavius.report.component.ReportStructureBuilder

class ApiIntegrationsReportScreen(private val navigator: Navigator) : ReportScreen() {
    override fun createReportStructure(): ReportStructureBuilder = ApiIntegrationsReportStructureBuilder(navigator)
    override val title = Translations.get("settings.api.title")
}