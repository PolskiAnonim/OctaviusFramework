package org.octavius.app

import androidx.compose.runtime.Composable
import org.octavius.navigation.Tab
import org.octavius.theme.AppTheme
import org.octavius.ui.screen.MainScreen

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