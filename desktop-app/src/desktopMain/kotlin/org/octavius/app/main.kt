package org.octavius.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.octavius.api.server.EmbeddedServer
import org.octavius.contract.FeatureModule
import org.octavius.localization.T
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.modules.games.GamesFeature
import org.octavius.modules.settings.SettingsFeature
import org.octavius.navigation.AppRouter
import org.octavius.navigation.NavigationEvent
import org.octavius.navigation.NavigationEventBus

/**
 * Funkcja główna aplikacji desktopowej.
 *
 * Inicjalizuje wszystkie komponenty aplikacji w następującej kolejności:
 * 1. Kontener zależności Koin z modułami bazy danych
 * 2. Rejestrację funkcjonalności (features) i ich komponentów
 * 3. System nawigacji AppRouter z zakładkami
 * 4. Serwer API Ktor w tle
 * 5. Główne okno aplikacji z interfejsem użytkownika
 *
 * Aplikacja używa architektury modularnej z oddzielnymi funkcjonalnościami:
 * - AsianMediaFeature - zarządzanie publikacjami azjatyckimi
 * - GamesFeature - zarządzanie grami
 * - SettingsFeature - ustawienia aplikacji (obecnie zakomentowane)
 */
fun main() = application {

    // Uruchom Koin
    startKoin {
        printLogger()
        modules(databaseModule)
    }

    // Rejestrujemy wszystkie funkcjonalności aplikacji.
    val features: List<FeatureModule> = listOf(
        AsianMediaFeature,
        GamesFeature,
        SettingsFeature
    )
    // ==========================================================


    // Automatyczne przetwarzanie listy modułów na konkretne komponenty
    val tabs = features.mapNotNull { it.getTab() }
    val apiModules = features.flatMap { it.getApiModules().orEmpty() }
    val screenFactories = features.flatMap { it.getScreenFactories().orEmpty() }.associateBy { it.screenId }

    // Inicjalizacja reszty aplikacji (kod pozostaje taki sam, ale używa powyższych list)
    val applicationScope = CoroutineScope(Dispatchers.Default)

    // Inicjalizacja routera nawigacji
    AppRouter.initialize(tabs)

    // Nasłuchiwanie na zdarzenia
    applicationScope.launch {
        NavigationEventBus.events.collectLatest { event ->
            when (event) {
                is NavigationEvent.SwitchToTab -> AppRouter.switchToTab(event.tabIndex)
                is NavigationEvent.NavigateToScreen -> {
                    screenFactories[event.screenId]?.let { factory ->
                        val screen = factory.create(event.payload)
                        AppRouter.navigateTo(screen)
                    } ?: println("Error: No factory for screenId: ${event.screenId}")
                }
            }
        }
    }

    // Uruchomienie serwera Ktor
    applicationScope.launch(Dispatchers.IO) {
        val server = EmbeddedServer(apiModules)
        server.run()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = T.get("app.name"),
        state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        App(tabs)
    }
}