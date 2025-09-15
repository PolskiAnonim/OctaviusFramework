package org.octavius.modules.settings.navigation

import androidx.compose.runtime.Composable
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

class SettingsTab : Tab {
    // Opcje nie są widoczne bezpośrednio
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = "",
        )

    override val index: UShort = 2u

    override val isVisibleInNavBar: Boolean
        get() = false

    override fun getInitialScreen(): Screen = SettingsScreen()
}