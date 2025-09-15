package org.octavius.modules.asian.navigation

import androidx.compose.runtime.Composable
import octavius.feature_asian_media.generated.resources.Res
import octavius.feature_asian_media.generated.resources.asian_media_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.localization.T
import org.octavius.modules.asian.home.ui.AsianMediaHomeScreen
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

class AsianMediaTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = T.get("tabs.asianMedia"),
            icon = painterResource(Res.drawable.asian_media_icon)
        )

    override val index: UShort = 0u

    override fun getInitialScreen(): Screen = AsianMediaHomeScreen.create()
}