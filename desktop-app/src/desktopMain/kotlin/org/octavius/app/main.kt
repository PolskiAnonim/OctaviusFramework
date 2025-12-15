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
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.octavius.api.contract.ApiModule
import org.octavius.api.server.EmbeddedServer
import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.data.DataAccess
import org.octavius.localization.T
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.modules.games.GamesFeature
import org.octavius.modules.settings.SettingsFeature
import org.octavius.navigation.AppRouter
import org.octavius.navigation.NavigationEvent
import org.octavius.navigation.NavigationEventBus
import org.octavius.navigation.Tab
import org.octavius.ui.theme.AppTheme
import java.awt.Frame

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

        when (appState) {
            AppState.Loading -> {
                AppLoadingScreen(
                    onLoaded = { appState = AppState.Ready },
                    koin = koin
                )
            }
            AppState.Ready -> {
                MainAppScreen(onCloseRequest = ::exitApplication)
            }
        }
    }
}

@Composable
private fun ApplicationScope.AppLoadingScreen(onLoaded: () -> Unit, koin: Koin) {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Loading...",
        state = rememberWindowState(position = WindowPosition(Alignment.Center), size = DpSize(300.dp, 200.dp)),
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
        @Suppress("HardcodedDispatcher")
        withContext(Dispatchers.IO) {
            koin.get<DataAccess>() // Używamy przekazanego Koin
        }
        onLoaded()
    }
}

@Composable
private fun ApplicationScope.MainAppScreen(onCloseRequest: () -> Unit) {
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

    AppLifecycleManager(apiModules, tabs)

    Window(
        onCloseRequest = ::exitApplication,
        title = T.get("app.name"),
        state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        NavigationHandler(screenFactories)

        // Renderowanie UI aplikacji
        App(tabs)
    }
}

@Composable
private fun AppLifecycleManager(apiModules: List<ApiModule>, tabs: List<Tab>) {
    val server = remember { EmbeddedServer(apiModules) }

    DisposableEffect(Unit) {
        AppRouter.initialize(tabs)

        println("Starting Ktor server in background...")

        // Uruchamiamy serwer w korutynie w tle
        val serverScope = CoroutineScope(Dispatchers.IO)
        val job = serverScope.launch {
            server.start()
        }

        onDispose {
            println("onDispose triggered. Shutting down server.")
            server.stop() // Grzecznie zatrzymujemy serwer
            job.cancel() // Anulujemy korutynę na wszelki wypadek
            serverScope.cancel() // Czyścimy cały scope
        }
    }
}

@Composable
private fun FrameWindowScope.NavigationHandler(screenFactories: Map<String, ScreenFactory>) {
    // Uzyskujemy dostęp do bazowego okna AWT
    val awtWindow = this.window

    LaunchedEffect(Unit) {
        NavigationEventBus.events.collectLatest { event ->
            when (event) {
                is NavigationEvent.Navigate -> {
                    if (event.screenId == null) {
                        AppRouter.switchToTab(event.tabId)
                    } else {
                        screenFactories[event.screenId]?.let { factory ->
                            val screen = factory.create(event.payload)
                            AppRouter.navigateTo(screen, event.tabId)
                        } ?: println("Error: No factory for screenId: ${event.screenId}")
                    }
                }
            }
            bringWindowToFront(awtWindow)
        }
    }
}

private fun bringWindowToFront(window: Frame) {
    if (window.extendedState == Frame.ICONIFIED) {
        window.extendedState = Frame.NORMAL
    }
    window.isAlwaysOnTop = true
    window.isAlwaysOnTop = false
    window.requestFocus()
}
