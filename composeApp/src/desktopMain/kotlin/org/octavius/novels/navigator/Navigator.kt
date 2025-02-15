package org.octavius.novels.navigator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class Navigator {
    private val stack= mutableStateListOf<Screen>()

    @Composable
    fun DisplayLast() {
        // Provide the current navigator instance if needed
        CompositionLocalProvider(LocalNavigator provides this) {
            Column {
                Row { NavigationBar() }
                Row { stack.last().Content() }
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
        TopAppBar(
            modifier = Modifier.height(56.dp),
            backgroundColor = MaterialTheme.colors.primary
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { this@Navigator.removeScreen() },
                    enabled = stack.size > 1,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}



