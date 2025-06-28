package org.octavius.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.octavius.modules.asian.ui.AsianMediaTab
import org.octavius.modules.games.ui.GameTab
import org.octavius.modules.settings.ui.SettingsTab
import org.octavius.navigator.TabNavigator
import org.octavius.ui.component.LocalSnackbarManager
import org.octavius.ui.component.SnackbarManager

/**
 * Główny ekran aplikacji - punkt wejścia UI zawierający nawigację między zakładkami.
 *
 * Singleton odpowiedzialny za:
 * - Konfigurację globalnego TabNavigator z dostępnymi zakładkami
 * - Inicjalizację SnackbarManager i CompositionLocalProvider
 * - Renderowanie Scaffold z obsługą snackbar
 * - Wyświetlanie aktywnej zakładki
 *
 * Lista zakładek:
 * - AsianMediaTab - zarządzanie publikacjami azjatyckimi
 * - GameTab - zarządzanie grami
 * - SettingsTab - ustawienia aplikacji
 */
object MainScreen {

    /** Lista wszystkich dostępnych zakładek w aplikacji */
    private val tabs = listOf(
        AsianMediaTab(), GameTab(), SettingsTab()
    )

    /** Navigator zarządzający przełączaniem między zakładkami */
    private val tabNavigator = TabNavigator(tabs)

    /** Globalny menedżer powiadomień snackbar */
    val snackbarManager = SnackbarManager()

    /**
     * Główny Composable renderujący interfejs aplikacji.
     *
     * Konfiguruje:
     * - CompositionLocalProvider z SnackbarManager
     * - Scaffold z obsługą snackbar
     * - Wyświetlanie aktywnej zakładki przez TabNavigator
     */
    @Composable
    fun Content() {
        CompositionLocalProvider(LocalSnackbarManager provides snackbarManager) {
            val snackbarHostState = remember { SnackbarHostState() }

            snackbarManager.HandleSnackbar(snackbarHostState)

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                Box(Modifier.fillMaxSize()) {
                    tabNavigator.Display()
                }
            }
        }
    }
}