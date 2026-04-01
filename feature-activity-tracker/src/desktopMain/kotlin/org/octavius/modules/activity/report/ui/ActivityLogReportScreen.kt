package org.octavius.modules.activity.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.activity.report.ActivityLogReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class ActivityLogReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.ActivityTracker.Report.activityLogTitle()
            val reportStructureBuilder = ActivityLogReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}
