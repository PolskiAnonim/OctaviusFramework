package org.octavius.modules.asian.navigation

import androidx.compose.runtime.Composable
import octavius.feature_asian_media.generated.resources.Res
import octavius.feature_asian_media.generated.resources.asian_media_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.contract.Screen
import org.octavius.contract.Tab
import org.octavius.contract.TabOptions
import org.octavius.localization.Translations
import org.octavius.modules.asian.report.ui.AsianMediaReportScreen

class AsianMediaTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Translations.get("tabs.asianMedia"),
            icon = painterResource(Res.drawable.asian_media_icon)
        )

    override val index: UShort = 0u

    override fun getInitialScreen(): Screen = AsianMediaReportScreen.create()
}