package org.octavius.navigator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations

/**
 * System nawigacji stosowy do zarządzania ekranami w aplikacji.
 *
 * Implementuje klasyczną nawigację push/pop z automatycznym paskiem nawigacji
 * zawierającym przycisk powrotu i tytuł aktualnego ekranu.
 */

/**
 * Navigator zarządzający stosem ekranów z automatycznym paskiem nawigacji.
 *
 * Funkcjonalności:
 * - Stos ekranów z push/pop semantyką
 * - Automatyczny pasek nawigacji z tytułem i przyciskiem powrotu
 * - Animowane przejścia między ekranami
 * - CompositionLocal provider dla dostępu z zagnieżdżonych komponentów
 *
 * Użycie:
 * ```kotlin
 * val navigator = Navigator()
 * navigator.addScreen(MyScreen())
 * navigator.Display()
 * ```
 */
class Navigator {
    /** Stos ekranów, ostatni element to aktualnie wyświetlany ekran */
    private val stack = mutableStateListOf<Screen>()

    /**
     * Główny Composable renderujący nawigator z paskiem nawigacji.
     *
     * Konfiguruje:
     * - CompositionLocalProvider z dostępem do nawigatora
     * - Scaffold z automatycznym paskiem nawigacji
     * - Wyświetlanie aktualnego ekranu ze stosu
     */
    @Composable
    fun Display() {
        // Provide the current navigator instance if needed
        CompositionLocalProvider(LocalNavigator provides this) {
            Scaffold(
                topBar = { NavigationBar() },
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    if (stack.isNotEmpty()) {
                        stack.last().Content()
                    }
                }
            }
        }
    }

    /**
     * Dodaje nowy ekran na szczyt stosu.
     *
     * Jeśli nowy ekran jest tego samego typu co aktualny,
     * zastępuje go zamiast dodawać nowy.
     *
     * @param screen Ekran do dodania
     */
    fun addScreen(screen: Screen) {
        if (stack.size > 1 && screen::class == stack.last()::class) {
            stack.removeLast()
            stack.add(screen)
        } else stack.add(screen)
    }

    /**
     * Usuwa ostatni ekran ze stosu (powrót do poprzedniego).
     *
     * Nie robi nic jeśli stos jest pusty.
     */
    fun removeScreen() {
        stack.removeLast()
    }

    /**
     * Pasek nawigacji wyświetlany na górze ekranu.
     *
     * Zawiera:
     * - Przycisk powrotu (widoczny gdy jest więcej niż 1 ekran)
     * - Tytuł aktualnego ekranu
     * - Animacje pokazywania/ukrywania przycisku powrotu
     */
    @Composable
    fun NavigationBar() {
        Surface(
            modifier = Modifier.height(56.dp), color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                IconButton(
                    onClick = { this@Navigator.removeScreen() },
                    enabled = stack.size > 1,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    AnimatedVisibility(
                        visible = stack.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = Translations.get("navigation.back"),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                if (stack.isNotEmpty()) {
                    Text(
                        text = stack.last().title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}