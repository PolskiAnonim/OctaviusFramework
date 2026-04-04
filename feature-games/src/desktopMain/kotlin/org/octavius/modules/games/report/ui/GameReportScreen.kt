package org.octavius.modules.games.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.games.report.GameReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class GameReportScreen {
    companion object {
        fun create(categoryId: Int? = null, seriesId: Int? = null): ReportScreen {
            val title = Tr.Games.Report.title()
            val reportStructureBuilder = GameReportStructureBuilder(categoryId, seriesId)
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}