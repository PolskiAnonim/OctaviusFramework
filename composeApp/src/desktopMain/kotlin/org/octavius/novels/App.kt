package org.octavius.novels

import androidx.compose.runtime.Composable
import org.octavius.novels.screens.MainScreen
import org.octavius.novels.theme.NovelsTheme


@Composable
fun App() {
    NovelsTheme(darkTheme = false) { // Ustaw false dla jasnego motywu
            MainScreen.Content()
    }
}