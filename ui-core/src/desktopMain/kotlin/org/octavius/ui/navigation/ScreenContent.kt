package org.octavius.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.contract.Screen
import org.octavius.localization.Translations

/**
 * Komponent wyświetlający zawartość bieżącego ekranu ze stosu nawigacji.
 *
 * Zarządza renderowaniem aktualnego ekranu,
 * który zawiera tytuł oraz przycisk "wstecz" (widoczny tylko, gdy na stosie
 * znajduje się więcej niż jeden ekran).
 *
 * @param screenStack Aktualny stos ekranów. Ostatni element na liście jest traktowany jako aktywny.
 * @param onBack Funkcja zwrotna wywoływana po naciśnięciu przycisku "wstecz".
 */
@Composable
fun ScreenContent(
    screenStack: List<Screen>,
    onBack: () -> Unit
) {
    val currentScreen = screenStack.lastOrNull() ?: return

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.height(56.dp), color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    IconButton(
                        onClick = { onBack() },
                        enabled = screenStack.size > 1,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        AnimatedVisibility(
                            visible = screenStack.size > 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Translations.get("navigation.back"),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    if (screenStack.isNotEmpty()) {
                        Text(
                            text = currentScreen.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(targetState = currentScreen) { screen ->
                screen.Content()
            }
        }
    }
}