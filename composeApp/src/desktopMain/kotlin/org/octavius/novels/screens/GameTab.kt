package org.octavius.novels.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import novelskotlin.composeapp.generated.resources.Res
import novelskotlin.composeapp.generated.resources.game_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.novels.navigator.Tab
import org.octavius.novels.navigator.TabOptions

class GameTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = "Gry",
            icon = painterResource(Res.drawable.game_icon)
        )

    override val index: UShort = 1u

    @Composable
    override fun Content() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Panel Gier - W trakcie implementacji")
        }
    }
}