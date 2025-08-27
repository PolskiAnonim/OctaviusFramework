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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.octavius.contract.Tab
import org.octavius.navigation.AppRouter
import org.octavius.ui.navigation.AppTabBar
import org.octavius.ui.navigation.ScreenContent
import org.octavius.ui.error.GlobalErrorDialog
import org.octavius.ui.error.GlobalErrorHandler
import org.octavius.ui.snackbar.SnackbarManager

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

    /**
     * Główny Composable renderujący interfejs aplikacji.
     *
     * Konfiguruje:
     * - CompositionLocalProvider z SnackbarManager
     * - Scaffold z obsługą snackbar
     * - Wyświetlanie aktywnej zakładki
     */
    @Composable
    fun Content(tabs: List<Tab>) {
        val navState by AppRouter.state.collectAsState()

        if (navState == null) return


        val snackbarHostState = remember { SnackbarHostState() }

        SnackbarManager.HandleSnackbar(snackbarHostState)

        val error by GlobalErrorHandler.errorDetails.collectAsState()
        error?.let { details ->
            GlobalErrorDialog(
                errorDetails = details,
                onDismiss = { GlobalErrorHandler.dismissError() }
            )
        }

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