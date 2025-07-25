package org.octavius.modules.settings.ui

import androidx.compose.runtime.Composable
import octavius.feature_settings.generated.resources.Res
import octavius.feature_settings.generated.resources.settings_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.contract.Screen
import org.octavius.contract.Tab
import org.octavius.contract.TabOptions
import org.octavius.localization.Translations

class SettingsTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Translations.get("tabs.settings"),
            icon = painterResource(Res.drawable.settings_icon)
        )

    override val index: UShort = 2u

    override fun getInitialScreen(): Screen = SettingsScreen()
}