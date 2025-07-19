package org.octavius.modules.asian.ui

import androidx.compose.runtime.Composable
import novelskotlin.composeapp.generated.resources.Res
import novelskotlin.composeapp.generated.resources.novel_icon
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
            icon = painterResource(Res.drawable.novel_icon)
        )

    override val index: UShort = 0u

    override fun getInitialScreen(): Screen = AsianMediaReportScreen()
}