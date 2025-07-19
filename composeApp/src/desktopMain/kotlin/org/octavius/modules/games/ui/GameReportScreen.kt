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

class GameReportScreen() : ReportScreen() {
    override val title = Translations.get("games.report.title")
    override fun createReportStructure(): ReportStructureBuilder = GameReportStructureBuilder()

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text(Translations.get("games.report.newGame")) },
            onClick = {
                AppRouter.navigateTo(
                    GameFormScreen(
                        onSaveSuccess = { AppRouter.goBack() },
                        onCancel = { AppRouter.goBack() }
                    ))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text(Translations.get("games.report.newSeries")) },
            onClick = {
                AppRouter.navigateTo(
                    GameSeriesFormScreen(
                        onSaveSuccess = { AppRouter.goBack() },
                        onCancel = { AppRouter.goBack() }
                    ))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        )
    }
}