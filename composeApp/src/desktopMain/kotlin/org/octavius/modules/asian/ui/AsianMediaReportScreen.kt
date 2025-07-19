package org.octavius.modules.asian.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.localization.Translations
import org.octavius.modules.asian.AsianMediaReportStructureBuilder
import org.octavius.navigation.AppRouter
import org.octavius.report.component.ReportScreen
import org.octavius.report.component.ReportStructureBuilder

class AsianMediaReportScreen() : ReportScreen() {
    override val title = Translations.get("asianMedia.report.title")
    override fun createReportStructure(): ReportStructureBuilder = AsianMediaReportStructureBuilder()

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text(Translations.get("asianMedia.report.newTitle")) },
            onClick = {
                AppRouter.navigateTo(
                    AsianMediaFormScreen(
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