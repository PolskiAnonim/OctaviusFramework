package org.octavius.modules.games.report.ui

import org.octavius.localization.Tr
import org.octavius.modules.games.report.GameCategoriesReportStructureBuilder
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen

class GameCategoriesReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = Tr.Games.Categories.title()
            val reportStructureBuilder = GameCategoriesReportStructureBuilder()
            return ReportScreen(title, ReportHandler(reportStructureBuilder))
        }
    }
}