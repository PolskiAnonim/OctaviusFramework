package org.octavius.novels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.octavius.novels.screens.MainScreen
import org.octavius.novels.state.LocalState
import org.octavius.novels.state.State
import org.octavius.novels.theme.NovelsTheme


@Composable
fun App() {
    NovelsTheme(darkTheme = false) { // Ustaw false dla jasnego motywu
        CompositionLocalProvider(LocalState provides State()) {
            MainScreen.Content()
        }
    }
}