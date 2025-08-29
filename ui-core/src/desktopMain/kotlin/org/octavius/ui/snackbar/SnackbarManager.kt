package org.octavius.ui.snackbar

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Wprowadzamy data class, żeby StateFlow reagował na nowe obiekty, a nie tylko na zmianę tekstu
private data class SnackbarEvent(val message: String, val id: Long = System.currentTimeMillis())

/**
 * Globalny singleton do zarządzania powiadomieniami typu Snackbar.
 * Dostępny z dowolnego miejsca w aplikacji.
 */
object SnackbarManager {

    private val _snackbarEvent = MutableStateFlow<SnackbarEvent?>(null)

    /**
     * Wyświetla Snackbar z podanym komunikatem.
     * @param message Tekst do wyświetlenia.
     */
    fun showMessage(message: String) {
        _snackbarEvent.value = SnackbarEvent(message)
    }

    /**
     * Composable, który nasłuchuje na nowe wiadomości i wyświetla je
     * przy użyciu dostarczonego SnackbarHostState.
     * Należy go umieścić w głównym komponencie UI aplikacji.
     */
    @Composable
    fun HandleSnackbar(snackbarHostState: SnackbarHostState) {
        val scope = rememberCoroutineScope()

        // Używamy LaunchedEffect i collectLatest, aby reagować na każde nowe zdarzenie,
        // nawet jeśli wiadomość jest taka sama.
        LaunchedEffect(Unit) {
            _snackbarEvent.collectLatest { event ->
                event?.let {
                    scope.launch {
                        snackbarHostState.showSnackbar(message = it.message)
                    }
                }
            }
        }
    }
}