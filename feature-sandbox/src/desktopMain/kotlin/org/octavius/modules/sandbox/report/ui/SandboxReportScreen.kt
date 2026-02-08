package org.octavius.modules.sandbox.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.sandbox.report.SandboxReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class SandboxReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Tr.Sandbox.Report.title()
            val reportStructureBuilder = SandboxReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}
