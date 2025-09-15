package org.octavius.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.startKoin
import org.octavius.api.server.EmbeddedServer
import org.octavius.contract.FeatureModule
import org.octavius.database.DatabaseSystem
import org.octavius.localization.T
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.modules.games.GamesFeature
import org.octavius.modules.settings.SettingsFeature
import org.octavius.navigation.AppRouter
import org.octavius.navigation.NavigationEvent
import org.octavius.navigation.NavigationEventBus
import org.octavius.ui.theme.AppTheme

// ... inne importy

// Prosty enum do zarządzania stanem aplikacji
private enum class AppState {
    Loading, // Aplikacja ładuje zasoby
    Ready    // Aplikacja jest gotowa do pracy
}

fun main() {
    val koin = startKoin {
        printLogger()
        modules(databaseModule)
    }.koin

    application {
        var appState by remember { mutableStateOf(AppState.Loading) }

        if (appState == AppState.Loading) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Loading...",
                state = rememberWindowState(
                    position = WindowPosition(Alignment.Center),
                    size = DpSize(300.dp, 200.dp)
                ),
                undecorated = true,
                resizable = false
            ) {
                AppTheme(isDarkTheme = true) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Loading...")
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                // Wykonaj ciężką pracę w tle
                withContext(Dispatchers.IO) {
                    koin.get<DatabaseSystem>()
                }
                // Zmień stan, aby wywołać rekompozycję
                appState = AppState.Ready
            }
        } else {
            // ==============================================================================
            //  GŁÓWNA APLIKACJA (komponowana tylko raz, gdy stan jest Ready)
            // ==============================================================================
            val features: List<FeatureModule> = remember {
                listOf(AsianMediaFeature, GamesFeature, SettingsFeature)
            }
            val (tabs, apiModules, screenFactories) = remember {
                val tabs = features.mapNotNull { it.getTab() }
                val apiModules = features.flatMap { it.getApiModules().orEmpty() }
                val screenFactories = features.flatMap { it.getScreenFactories().orEmpty() }.associateBy { it.screenId }
                Triple(tabs, apiModules, screenFactories)
            }

            LaunchedEffect(tabs) { // Używamy LaunchedEffect, żeby zainicjować logikę tylko raz
                val applicationScope = CoroutineScope(Dispatchers.Default)
                AppRouter.initialize(tabs)

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
                applicationScope.launch(Dispatchers.IO) {
                    val server = EmbeddedServer(apiModules)
                    server.run()
                }
            }

            Window(
                onCloseRequest = ::exitApplication,
                title = T.get("app.name"),
                state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
            ) {
                App(tabs)
            }
        }
    }
}