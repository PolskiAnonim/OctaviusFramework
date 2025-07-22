package org.octavius.ui.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.octavius.notifier.Notifier

/**
 * Globalny menedżer powiadomień snackbar używany w całej aplikacji.
 * 
 * Zapewnia centralizowane zarządzanie wyświetlaniem komunikatów snackbar
 * z możliwością wywołania z dowolnego miejsca w aplikacji przez CompositionLocal.
 *
 */
class SnackbarManager: Notifier {
    private val _snackbarMessage = mutableStateOf("")
    private val _showSnackbar = mutableStateOf(false)
    
    /** Aktualny komunikat snackbar */
    val snackbarMessage: State<String> = _snackbarMessage
    
    /** Czy snackbar powinien być wyświetlony */
    val showSnackbar: State<Boolean> = _showSnackbar

    /**
     * Wyświetla snackbar z podanym komunikatem.
     * 
     * @param message Tekst komunikatu do wyświetlenia
     */
    override fun showMessage(message: String) {
        _snackbarMessage.value = message
        _showSnackbar.value = true
    }

    /**
     * Composable odpowiedzialny za obsługę wyświetlania snackbar.
     * 
     * Należy wywołać w głównym UI aby aktywować automatyczne wyświetlanie
     * komunikatów gdy stan showSnackbar się zmienia.
     * 
     * @param snackbarHostState SnackbarHostState używany do wyświetlania komunikatów
     */
    @Composable
    fun HandleSnackbar(snackbarHostState: SnackbarHostState) {
        val scope = rememberCoroutineScope()
        
        if (_showSnackbar.value) {
            LaunchedEffect(_snackbarMessage.value) {
                scope.launch {
                    snackbarHostState.showSnackbar(message = _snackbarMessage.value)
                    _showSnackbar.value = false
                }
            }
        }
    }
}