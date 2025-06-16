package org.octavius.novels.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.novels.domain.game.GameReport
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.components.ReportScreen

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