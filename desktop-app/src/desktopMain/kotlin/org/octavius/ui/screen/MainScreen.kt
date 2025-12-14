package org.octavius.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.octavius.dialog.DialogWrapper
import org.octavius.dialog.GlobalDialogManager
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Tab
import org.octavius.ui.navigation.AppTopBar
import org.octavius.ui.navigation.MainTopAppBar
import org.octavius.ui.navigation.TabContent
import org.octavius.ui.snackbar.SnackbarManager

/**
 * Główny ekran aplikacji - punkt wejścia UI zawierający nawigację między zakładkami.
 *
 * Singleton odpowiedzialny za:
 * - Obsługę nawigacji między zakładkami z animowanymi przejściami
 * - Zarządzanie stosami nawigacji dla każdej zakładki
 * - Inicjalizację menedżera snackbarów i dialogów
 * - Renderowanie głównego układu z paskiem górnym i zakładkami
 *
 */
object MainScreen {

    /**
     * Główny komponent Compose renderujący interfejs aplikacji.
     *
     * Konfiguruje kompletny układ aplikacji:
     * - Scaffold z paskiem górnym i obsługą snackbarów
     * - Pasek zakładek umożliwiający przełączanie między sekcjami
     * - Animowane przejścia między zakładkami z efektem przesuwania
     * - Menedżer globalnych dialogów
     *
     * @param tabs Lista dostępnych zakładek do wyświetlenia
     */
    @Composable
    fun Content(tabs: List<Tab>) {
        val navState by AppRouter.state.collectAsState()

        if (navState == null) return
        val visibleTabs = remember { tabs.filter { it.isVisibleInNavBar } }
        val currentScreen = navState!!.tabStacks[navState!!.activeTab]?.lastOrNull()
        val screenStackSize = navState!!.tabStacks[navState!!.activeTab]?.size ?: 0

        val snackbarHostState = remember { SnackbarHostState() }

        SnackbarManager.HandleSnackbar(snackbarHostState)

        val dialog by GlobalDialogManager.dialogConfig.collectAsState()
        DialogWrapper(config = dialog)

        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = currentScreen?.title ?: "",
                    showBackButton = screenStackSize > 1,
                    onBackClicked = { AppRouter.goBack() },
                    onSettingsClicked = { AppRouter.switchToTab("settings") }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().padding(paddingValues)) {
                AppTopBar(
                    tabs = visibleTabs,
                    currentState = navState!!,
                    onTabSelected = { tab -> AppRouter.switchToTab(tab) }
                )

                AnimatedContent(
                    targetState = navState!!.activeTab,
                    transitionSpec = {
                        // Znajdź indeksy starego i nowego taba na liście widocznych zakładek
                        val initialIndex = visibleTabs.indexOf(initialState)
                        val targetIndex = visibleTabs.indexOf(targetState)

                        // Jeśli któryś tab nie jest na liście (np. ukryty), nie animuj
                        if (initialIndex == -1 || targetIndex == -1) {
                            fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                        } else {
                            // Użyj znalezionych indeksów do określenia kierunku
                            val direction = if (targetIndex > initialIndex) {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            }

                            slideIntoContainer(
                                towards = direction, animationSpec = tween(300)
                            ) togetherWith slideOutOfContainer(
                                towards = direction, animationSpec = tween(300)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { targetTabIndex ->
                    val activeStack = navState!!.tabStacks[targetTabIndex] ?: emptyList()
                    if (activeStack.isNotEmpty()) {
                        TabContent(screenStack = activeStack)
                    }
                }
            }
        }
    }
}