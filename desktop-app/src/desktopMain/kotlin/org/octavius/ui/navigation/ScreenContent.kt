package org.octavius.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.navigation.Screen

/**
 * Komponent wyświetlający zawartość bieżącego ekranu ze stosu nawigacji.
 *
 * Zarządza renderowaniem aktualnego ekranu,
 * który zawiera tytuł oraz przycisk "wstecz" (widoczny tylko, gdy na stosie
 * znajduje się więcej niż jeden ekran).
 *
 * @param screenStack Aktualny stos ekranów. Ostatni element na liście jest traktowany jako aktywny.
 */
@Composable
fun TabContent(screenStack: List<Screen>) {
    val currentScreen = screenStack.lastOrNull() ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(targetState = currentScreen) { screen ->
            screen.Content()
        }
    }
}