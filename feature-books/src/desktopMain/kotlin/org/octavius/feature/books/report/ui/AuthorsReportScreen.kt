package org.octavius.feature.books.report.ui

import org.octavius.feature.books.report.AuthorsReportStructureBuilder
import org.octavius.localization.Tr
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class AuthorsReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.Books.Authors.Report.title()
            val reportStructureBuilder = AuthorsReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}