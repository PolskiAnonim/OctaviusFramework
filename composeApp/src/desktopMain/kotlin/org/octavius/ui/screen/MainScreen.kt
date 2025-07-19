package org.octavius.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.octavius.modules.asian.ui.AsianMediaTab
import org.octavius.modules.games.ui.GameTab
import org.octavius.modules.settings.ui.SettingsTab
import org.octavius.navigation.AppRouter
import org.octavius.navigation.AppTabBar
import org.octavius.navigation.ScreenContent
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

    init {
        AppRouter.initialize(tabs)
    }

    /** Globalny menedżer powiadomień snackbar */
    val snackbarManager = SnackbarManager()

    /**
     * Główny Composable renderujący interfejs aplikacji.
     *
     * Konfiguruje:
     * - CompositionLocalProvider z SnackbarManager
     * - Scaffold z obsługą snackbar
     * - Wyświetlanie aktywnej zakładki
     */
    @Composable
    fun Content() {
        val navState by AppRouter.state.collectAsState()

        if (navState == null) return

        CompositionLocalProvider(LocalSnackbarManager provides snackbarManager) {
            val snackbarHostState = remember { SnackbarHostState() }
            snackbarManager.HandleSnackbar(snackbarHostState)

            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
                Column(Modifier.fillMaxSize().padding(paddingValues)) {
                    AppTabBar(
                        tabs = tabs,
                        currentState = navState!!,
                        onTabSelected = { tabIndex -> AppRouter.switchToTab(tabIndex) }
                    )

                    AnimatedContent(
                        targetState = navState!!.activeTab.index,
                        transitionSpec = {
                            // Ta sama logika co w starym TabNavigatorze
                            val direction = if (targetState > initialState) {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            }

                            slideIntoContainer(
                                towards = direction, animationSpec = tween(300)
                            ) togetherWith slideOutOfContainer(
                                towards = direction, animationSpec = tween(300)
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { targetTabIndex ->
                        val activeStack = navState!!.tabStacks[targetTabIndex] ?: emptyList()

                        if (activeStack.isNotEmpty()) {
                            ScreenContent(
                                screenStack = activeStack,
                                onBack = { AppRouter.goBack() }
                            )
                        }
                    }
                }
            }
        }
    }
}