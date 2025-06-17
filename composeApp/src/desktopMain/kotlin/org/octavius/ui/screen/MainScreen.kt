package org.octavius.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.navigator.TabNavigator
import org.octavius.ui.screen.tab.GameTab
import org.octavius.ui.screen.tab.NovelTab

object MainScreen {
    private val tabs = listOf(
        NovelTab(),
        GameTab()
    )

    private val tabNavigator = TabNavigator(tabs)

    @Composable
    fun Content() {
        Box(Modifier.Companion.fillMaxSize()) {
            tabNavigator.Display()
        }
    }
}