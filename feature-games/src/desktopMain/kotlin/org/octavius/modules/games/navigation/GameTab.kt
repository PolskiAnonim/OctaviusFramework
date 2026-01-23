package org.octavius.modules.games.navigation

import androidx.compose.runtime.Composable
import octavius.feature_games.generated.resources.Res
import octavius.feature_games.generated.resources.game_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

class GameTab : Tab {
    override val id = "games"

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Tr.Tabs.games(),
            icon = painterResource(Res.drawable.game_icon)
        )

    override fun getInitialScreen(): Screen = GamesHomeScreen.create()
}