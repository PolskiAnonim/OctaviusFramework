package org.octavius.novels.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import novelskotlin.composeapp.generated.resources.Res
import novelskotlin.composeapp.generated.resources.game_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.navigator.Tab
import org.octavius.novels.navigator.TabOptions

class GameTab : Tab {
    private val navigator = Navigator()

    init {
        navigator.addScreen(
            GameReportScreen(
                navigator
            )
        )
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = "Gry",
            icon = painterResource(Res.drawable.game_icon)
        )

    override val index: UShort = 1u

    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize()) {
            navigator.Display()
        }
    }
}