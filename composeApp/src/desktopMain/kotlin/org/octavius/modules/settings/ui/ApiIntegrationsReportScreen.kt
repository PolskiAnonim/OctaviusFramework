package org.octavius.modules.settings.ui

import org.octavius.localization.Translations
import org.octavius.modules.settings.ApiIntegrationsReportStructureBuilder
import org.octavius.report.component.ReportScreen
import org.octavius.report.component.ReportStructureBuilder

class ApiIntegrationsReportScreen() : ReportScreen() {
    override fun createReportStructure(): ReportStructureBuilder = ApiIntegrationsReportStructureBuilder()
    override val title = Translations.get("settings.api.title")
}