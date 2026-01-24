package org.octavius.modules.asian.navigation

import androidx.compose.runtime.Composable
import octavius.feature_asian_media.generated.resources.Res
import octavius.feature_asian_media.generated.resources.asian_media_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.localization.Tr
import org.octavius.modules.asian.home.ui.AsianMediaHomeScreen
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

class AsianMediaTab : Tab {
    override val id = "asian_media"

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Tr.Tabs.asianMedia(),
            icon = painterResource(Res.drawable.asian_media_icon)
        )

    override fun getInitialScreen(): Screen = AsianMediaHomeScreen.create()
}