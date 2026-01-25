package org.octavius.feature.books.report.ui

import org.octavius.feature.books.report.BooksReportStructureBuilder
import org.octavius.localization.Tr
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class BooksReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.Books.Report.title()
            val reportStructureBuilder = BooksReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}