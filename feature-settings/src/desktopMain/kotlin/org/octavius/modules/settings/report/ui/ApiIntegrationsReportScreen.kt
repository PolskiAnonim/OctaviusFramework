package org.octavius.modules.settings.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.settings.report.ApiIntegrationsReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class ApiIntegrationsReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Tr.Settings.Api.title()
            val reportStructure = ApiIntegrationsReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructure))
        }
    }
}