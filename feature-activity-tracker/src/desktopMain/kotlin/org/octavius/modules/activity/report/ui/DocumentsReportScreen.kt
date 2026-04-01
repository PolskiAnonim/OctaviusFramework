package org.octavius.modules.activity.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.activity.report.DocumentsReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class DocumentsReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.ActivityTracker.Report.documentsTitle()
            val reportStructureBuilder = DocumentsReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}
