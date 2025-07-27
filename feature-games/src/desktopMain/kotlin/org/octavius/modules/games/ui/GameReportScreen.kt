package org.octavius.modules.games.ui

import org.octavius.localization.Translations
import org.octavius.modules.games.GameReportStructureBuilder
import org.octavius.report.component.ReportScreen

class GameReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Translations.get("games.report.title")
            val reportStructureBuilder = GameReportStructureBuilder()
            return ReportScreen(title, reportStructureBuilder)
        }
    }
}