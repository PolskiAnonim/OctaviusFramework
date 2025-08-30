package org.octavius.app

import androidx.compose.runtime.Composable
import org.octavius.navigation.Tab
import org.octavius.ui.screen.MainScreen
import org.octavius.ui.theme.AppTheme

/**
 * Główny komponent aplikacji desktopowej.
 *
 * Odpowiada za inicjalizację motywu aplikacji i renderowanie głównego ekranu
 * z przekazanymi zakładkami. Aplikuje domyślny jasny motyw Material 3.
 *
 * @param tabs Lista zakładek [Tab] dostępnych w aplikacji
 */
@Composable
fun App(tabs: List<Tab>) {
    AppTheme(isDarkTheme = false) { // Ustaw false dla jasnego motywu
        MainScreen.Content(tabs)
    }
}