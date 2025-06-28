package org.octavius.ui.screen.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.games.GameReportHandler
import org.octavius.localization.Translations
import org.octavius.navigator.Navigator
import org.octavius.report.component.ReportScreen
import org.octavius.ui.screen.form.GameFormScreen
import org.octavius.ui.screen.form.GameSeriesFormScreen

class GameReportScreen(private val navigator: Navigator) : ReportScreen() {
    override val title = Translations.get("games.report.title")
    override val reportHandler = GameReportHandler(navigator)

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text(Translations.get("games.report.newGame")) },
            onClick = {
                navigator.addScreen(GameFormScreen(
                    onSaveSuccess = { navigator.removeScreen() },
                    onCancel = { navigator.removeScreen() }
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
                navigator.addScreen(GameSeriesFormScreen(
                    onSaveSuccess = { navigator.removeScreen() },
                    onCancel = { navigator.removeScreen() }
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