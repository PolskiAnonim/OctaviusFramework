package org.octavius.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.octavius.navigator.TabNavigator
import org.octavius.ui.component.LocalSnackbarManager
import org.octavius.ui.component.SnackbarManager
import org.octavius.ui.screen.tab.GameTab
import org.octavius.ui.screen.tab.NovelTab
import org.octavius.ui.screen.tab.SettingsTab

// Główny ekran, na nim zawsze wyświetla się aplikacja
object MainScreen {

    // Aktualnie
    private val tabs = listOf(
        NovelTab(),
        GameTab(),
        SettingsTab()
    )

    private val tabNavigator = TabNavigator(tabs)

    val snackbarManager = SnackbarManager()

    @Composable
    fun Content() {
        CompositionLocalProvider(LocalSnackbarManager provides snackbarManager) {
            val snackbarHostState = remember { SnackbarHostState() }

            snackbarManager.HandleSnackbar(snackbarHostState)

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Box(Modifier.fillMaxSize()) {
                    tabNavigator.Display()
                }
            }
        }

    }
}