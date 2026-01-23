package org.octavius.modules.asian.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.asian.report.AsianMediaReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class AsianMediaReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Tr.AsianMedia.Report.title()
            val builder = AsianMediaReportStructureBuilder()
            return ReportScreen(title, ReportHandler(builder))
        }
    }
}