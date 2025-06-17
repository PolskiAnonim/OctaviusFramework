package org.octavius.ui.screen.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.domain.game.GameReport
import org.octavius.navigator.Navigator
import org.octavius.report.components.ReportScreen
import org.octavius.ui.screen.form.GameFormScreen
import org.octavius.ui.screen.form.GameSeriesFormScreen

class GameReportScreen(private val navigator: Navigator) : ReportScreen() {
    
    override val report = GameReport(navigator)

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text("Nowa gra") },
            onClick = {
                navigator.addScreen(GameFormScreen())
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Nowa seria") },
            onClick = {
                navigator.addScreen(GameSeriesFormScreen())
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