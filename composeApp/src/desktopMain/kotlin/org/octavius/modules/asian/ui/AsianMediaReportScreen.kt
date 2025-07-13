package org.octavius.modules.asian.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.localization.Translations
import org.octavius.modules.asian.AsianMediaReportStructureBuilder
import org.octavius.navigator.Navigator
import org.octavius.report.component.ReportScreen
import org.octavius.report.component.ReportStructureBuilder

class AsianMediaReportScreen(private val navigator: Navigator) : ReportScreen() {
    override val title = Translations.get("asianMedia.report.title")
    override fun createReportStructure(): ReportStructureBuilder = AsianMediaReportStructureBuilder(navigator)

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text(Translations.get("asianMedia.report.newTitle")) },
            onClick = {
                navigator.addScreen(
                    AsianMediaFormScreen(
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