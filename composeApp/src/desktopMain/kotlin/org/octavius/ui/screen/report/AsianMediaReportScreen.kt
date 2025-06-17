package org.octavius.ui.screen.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.domain.asian.AsianMediaReport
import org.octavius.navigator.Navigator
import org.octavius.report.components.ReportScreen
import org.octavius.ui.screen.form.AsianMediaFormScreen

class AsianMediaReportScreen(private val navigator: Navigator) : ReportScreen() {
    
    override val report = AsianMediaReport(navigator)

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text("Nowa nowelka") },
            onClick = {
                navigator.addScreen(AsianMediaFormScreen())
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