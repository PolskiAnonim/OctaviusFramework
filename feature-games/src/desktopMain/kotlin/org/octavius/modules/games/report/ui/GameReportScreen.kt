package org.octavius.modules.games.report.ui

import org.octavius.localization.T
import org.octavius.modules.games.report.GameReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class GameReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = T.get("games.report.title")
            val reportStructureBuilder = GameReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}