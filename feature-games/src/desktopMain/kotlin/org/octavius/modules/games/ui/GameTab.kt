package org.octavius.modules.games.ui

import androidx.compose.runtime.Composable
import octavius.feature_games.generated.resources.Res
import octavius.feature_games.generated.resources.game_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.contract.Screen
import org.octavius.contract.Tab
import org.octavius.contract.TabOptions
import org.octavius.localization.Translations

class GameTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Translations.get("tabs.games"),
            icon = painterResource(Res.drawable.game_icon)
        )

    override val index: UShort = 1u

    override fun getInitialScreen(): Screen = GameReportScreen.create()
}