package org.octavius.modules.activity.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.activity.report.CategoriesReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class CategoriesReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.ActivityTracker.Category.title()
            val reportStructureBuilder = CategoriesReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}
