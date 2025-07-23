package org.octavius.modules.asian.ui

import androidx.compose.runtime.Composable
import octavius.feature_asian_media.generated.resources.Res
import octavius.feature_asian_media.generated.resources.asian_media_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.localization.Translations
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

class AsianMediaTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Translations.get("tabs.asianMedia"),
            icon = painterResource(Res.drawable.asian_media_icon)
        )

    override val index: UShort = 0u

    override fun getInitialScreen(): Screen = AsianMediaReportScreen()
}