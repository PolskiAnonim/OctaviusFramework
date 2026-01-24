package org.octavius.modules.games.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.games.report.GameSeriesReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class GameSeriesReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.Games.Series.title()
            val reportStructureBuilder = GameSeriesReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}