package org.octavius.ui.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

class SnackbarManager {
    private val _snackbarMessage = mutableStateOf("")
    private val _showSnackbar = mutableStateOf(false)
    
    val snackbarMessage: State<String> = _snackbarMessage
    val showSnackbar: State<Boolean> = _showSnackbar

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
        _showSnackbar.value = true
    }

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

val LocalSnackbarManager = compositionLocalOf<SnackbarManager> { 
    error("SnackbarManager not provided") 
}