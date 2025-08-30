package org.octavius.modules.settings.report.ui

import org.octavius.localization.T
import org.octavius.modules.settings.report.ApiIntegrationsReportStructureBuilder
import org.octavius.report.component.ReportScreen

class ApiIntegrationsReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = T.get("settings.api.title")
            val reportStructure = ApiIntegrationsReportStructureBuilder()
            return ReportScreen(title, reportStructure)
        }
    }
}