package org.octavius.modules.games.report.ui

import org.octavius.localization.T
import org.octavius.modules.games.report.GameSeriesReportStructureBuilder
import org.octavius.report.component.ReportScreen

class GameSeriesReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = T.get("games.series.title")
            val reportStructureBuilder = GameSeriesReportStructureBuilder()
            return ReportScreen(title, reportStructureBuilder)
        }
    }
}