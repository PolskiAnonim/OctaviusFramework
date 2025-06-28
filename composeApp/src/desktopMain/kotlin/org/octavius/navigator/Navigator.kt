package org.octavius.navigator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
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

class Navigator {
    private val stack = mutableStateListOf<Screen>()

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

    fun addScreen(screen: Screen) {
        if (stack.size > 1 && screen::class == stack.last()::class) {
            stack.removeLast()
            stack.add(screen)
        } else
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