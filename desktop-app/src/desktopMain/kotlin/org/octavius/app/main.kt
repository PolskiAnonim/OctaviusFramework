package org.octavius.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.octavius.contract.FeatureModule
import org.octavius.localization.Translations
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.modules.games.GamesFeature
import org.octavius.modules.settings.SettingsFeature
import org.octavius.navigation.AppRouter

fun main() = application {
    // Rejestrujemy wszystkie funkcjonalności aplikacji.
    val features: List<FeatureModule> = listOf(
        AsianMediaFeature,
        GamesFeature,
        SettingsFeature
    )
    // ==========================================================


    // Automatyczne przetwarzanie listy modułów na konkretne komponenty
    val tabs = features.mapNotNull { it.getTab() }


    // Inicjalizacja routera nawigacji
    AppRouter.initialize(tabs)


    Window(
        onCloseRequest = ::exitApplication,
        title = Translations.get("app.name"),
        state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        App(tabs)
    }
}