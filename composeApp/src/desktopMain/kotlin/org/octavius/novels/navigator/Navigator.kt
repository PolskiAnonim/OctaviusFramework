package org.octavius.novels.navigator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class Navigator {
    private val stack = mutableStateListOf<Screen>()

    // Obsługa Snackbara
    private val _snackbarMessage = mutableStateOf("")
    private val _showSnackbar = mutableStateOf(false)

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
        _showSnackbar.value = true
    }

    @Composable
    fun DisplayLast() {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Pokaż snackbar, jeśli _showSnackbar jest true
        if (_showSnackbar.value) {
            scope.launch {
                snackbarHostState.showSnackbar(message = _snackbarMessage.value)
                _showSnackbar.value = false
            }
        }

        // Provide the current navigator instance if needed
        CompositionLocalProvider(LocalNavigator provides this) {
            Scaffold(
                topBar = { NavigationBar() },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                if (stack.isNotEmpty()) {
                    stack.last().Content(paddingValues)
                }
            }
        }
    }

    fun AddScreen(screen: Screen) {
        if (stack.size>1 && screen::class == stack.last()::class) {
            stack.removeLast()
            stack.add(screen)
        }
        else
            stack.add(screen)
    }

    fun removeScreen() {
        stack.removeLast()
    }

    @Composable
    fun NavigationBar() {
        Surface(
            modifier = Modifier.height(56.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { this@Navigator.removeScreen() },
                    enabled = stack.size > 1,
                ) {
                    AnimatedVisibility(
                        visible = stack.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wstecz",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}