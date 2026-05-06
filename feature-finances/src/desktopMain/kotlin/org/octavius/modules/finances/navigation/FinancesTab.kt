package org.octavius.modules.finances.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.octavius.localization.Tr
import org.octavius.modules.finances.report.AccountReportStructureBuilder
import org.octavius.modules.finances.report.TransactionReportStructureBuilder
import org.octavius.modules.finances.ui.FinancesHomeScreen
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions
import org.octavius.report.component.ReportScreen

class FinancesTab : Tab {
    override val id: String get() = "finances"

    override val options: TabOptions
        @Composable get() = TabOptions(
            title = Tr.Finances.title(),
            icon = rememberVectorPainter(Icons.Default.Payments)
        )

    override fun getInitialScreen(): Screen = FinancesHomeScreen()
}
