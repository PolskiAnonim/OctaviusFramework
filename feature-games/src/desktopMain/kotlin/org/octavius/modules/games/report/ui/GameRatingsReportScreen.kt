package org.octavius.modules.games.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.games.report.GameRatingsReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class GameRatingsReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Tr.Games.Details.title()
            val reportStructureBuilder = GameRatingsReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}