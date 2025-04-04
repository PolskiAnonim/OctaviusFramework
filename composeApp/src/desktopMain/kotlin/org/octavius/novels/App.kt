package org.octavius.novels

import androidx.compose.runtime.*
import org.octavius.novels.state.LocalState
import org.octavius.novels.state.State
import org.octavius.novels.theme.NovelsTheme
import org.octavius.novels.screens.MainScreen


@Composable
fun App() {
    NovelsTheme(darkTheme = false) { // Ustaw false dla jasnego motywu
        CompositionLocalProvider(LocalState provides State()) {
            MainScreen.Content()
        }
    }
}