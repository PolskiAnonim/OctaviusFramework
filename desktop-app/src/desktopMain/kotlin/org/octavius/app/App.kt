package org.octavius.app

import androidx.compose.runtime.Composable
import org.octavius.ui.screen.MainScreen
import org.octavius.ui.theme.AppTheme


@Composable
fun App() {
    AppTheme(isDarkTheme = false) { // Ustaw false dla jasnego motywu
        MainScreen.Content()
    }
}