package org.octavius.modules.games.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.localization.Translations
import org.octavius.modules.games.GameReportStructureBuilder
import org.octavius.navigation.AppRouter
import org.octavius.report.component.ReportScreen
import org.octavius.report.component.ReportStructureBuilder

class GameReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Translations.get("games.report.title")
            val reportStructureBuilder = GameReportStructureBuilder()
            return ReportScreen(title, reportStructureBuilder)
        }
    }
}