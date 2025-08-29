package org.octavius.modules.games.navigation

import androidx.compose.runtime.Composable
import octavius.feature_games.generated.resources.Res
import octavius.feature_games.generated.resources.game_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.localization.Translations
import org.octavius.modules.games.report.ui.GameReportScreen
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

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