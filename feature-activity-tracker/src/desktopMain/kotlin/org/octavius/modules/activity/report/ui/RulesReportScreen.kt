package org.octavius.modules.activity.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.activity.report.RulesReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class RulesReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.ActivityTracker.Rule.title()
            val reportStructureBuilder = RulesReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}
