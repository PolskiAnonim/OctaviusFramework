package org.octavius.novels.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.novels.navigator.TabNavigator

object MainScreen {
    private val tabs = listOf(
        NovelTab(),
        GameTab()
    )

    private val tabNavigator = TabNavigator(tabs)

    @Composable
    fun Content() {
        Box(Modifier.fillMaxSize()) {
            tabNavigator.Display()
        }
    }
}