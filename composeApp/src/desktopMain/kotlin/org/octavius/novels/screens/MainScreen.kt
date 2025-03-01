package org.octavius.novels.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.novels.navigator.Navigator

object MainScreen {

    var navigator: Navigator = Navigator()

    init {
        navigator.AddScreen(NovelMainScreen())
    }

    @Composable
    fun Content() {
        Box(Modifier.fillMaxSize()) {
            navigator.DisplayLast()
        }
    }
}