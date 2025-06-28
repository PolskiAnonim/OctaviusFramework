package org.octavius.navigator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * System nawigacji tabularnej z animowanymi przejściami między zakładkami.
 *
 * Zapewnia klasyczną nawigację za pomocą zakładek z paskiem u góry ekranu
 * i animowanym przełączaniem zawartości.
 */

/**
 * Navigator tabularny zarządzający zakładkami z animowanymi przejściami.
 *
 * Funkcjonalności:
 * - Pasek zakładek z tytułami i ikonami
 * - Animowane przejścia slide między zakładkami
 * - Wygląd dostosowany do Material 3
 * - Obsługa kliknięć i zmiany aktywnej zakładki
 *
 * @param tabs Lista zakładek do wyświetlenia
 * @param initialIndex Początkowy indeks aktywnej zakładki (domyślnie 0)
 *
 * Przykład użycia:
 * ```kotlin
 * val navigator = TabNavigator(listOf(tab1, tab2, tab3))
 * navigator.Display()
 * ```
 */
class TabNavigator(
    private val tabs: List<Tab>,
    initialIndex: UShort = 0u,
) {
    /** Indeks aktualnie wybranej zakładki */
    private val currentIndexState: MutableState<UShort> = mutableStateOf(initialIndex)

    /**
     * Indeks aktualnie wybranej zakładki.
     *
     * Zmiana tej wartości automatycznie przełącza na inną zakładkę.
     */
    var currentIndex: UShort
        get() = currentIndexState.value
        set(value) {
            currentIndexState.value = value
        }

    /**
     * Aktualnie wybrana zakładka.
     *
     * @return Tab odpowiadający currentIndex
     */
    val current: Tab
        @Composable get() = tabs.first { it.index == currentIndex }

    /**
     * Główny Composable renderujący navigator z paskiem zakładek i zawartością.
     *
     * Struktura:
     * - Pasek zakładek u góry
     * - Animowana zawartość aktywnej zakładki
     *
     * Animacje:
     * - Slide w lewo przy przełączaniu na wyższe indeksy
     * - Slide w prawo przy przełączaniu na niższe indeksy
     * - Czas trwania: 300ms
     */
    @Composable
    fun Display() {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pasek zakładek
            TabBar()

            // Zawartość aktualnej zakładki z animacją
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
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
            ) { targetIndex ->
                Box(modifier = Modifier.fillMaxSize()) {
                    tabs.first { it.index == targetIndex }.Content()
                }
            }
        }
    }

    /**
     * Pasek zakładek wyświetlany u góry naviogatora.
     *
     * Dla każdej zakładki wyświetla:
     * - Ikonę (jeśli jest dostępna)
     * - Tytuł
     * - Odpowiednie kolory dla aktywnej/nieaktywnej zakładki
     * - Interaktywność (kliknięcie zmienia aktywną zakładkę)
     */
    @Composable
    private fun TabBar() {
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp), color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.index == currentIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { currentIndex = tab.index }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.primary
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            tab.options.icon?.let {
                                Icon(
                                    painter = it,
                                    contentDescription = tab.options.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Text(
                                text = tab.options.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}