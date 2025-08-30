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
 * Zarządza renderowaniem aktualnego ekranu z animowanymi przejściami między ekranami.
 * Automatycznie wybiera ostatni ekran ze stosu jako aktywny do wyświetlenia.
 *
 * @param screenStack Aktualny stos ekranów, gdzie ostatni element jest aktywny
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