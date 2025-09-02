package org.octavius.modules.games.report.ui

import org.octavius.localization.T
import org.octavius.modules.games.report.GameCategoriesReportStructureBuilder
import org.octavius.report.component.ReportScreen

class GameCategoriesReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title: String = T.get("games.categories.title")
            val reportStructureBuilder = GameCategoriesReportStructureBuilder()
            return ReportScreen(title, reportStructureBuilder)
        }
    }
}