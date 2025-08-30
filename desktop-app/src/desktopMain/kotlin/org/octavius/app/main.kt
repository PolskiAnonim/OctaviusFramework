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
import org.octavius.localization.Translations
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.modules.games.GamesFeature
import org.octavius.modules.settings.SettingsFeature
import org.octavius.navigation.AppRouter
import org.octavius.navigation.NavigationEvent
import org.octavius.navigation.NavigationEventBus

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
        //SettingsFeature
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
        title = Translations.get("app.name"),
        state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        App(tabs)
    }
}