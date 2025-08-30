package org.octavius.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import org.octavius.localization.T

// W ui/navigation/MainTopAppBar.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    showBackButton: Boolean,
    onBackClicked: () -> Unit,
    onSettingsClicked: () -> Unit
) {
    CenterAlignedTopAppBar( // <-- Zmiana tutaj!
        title = {
            // Animujemy treść tytułu, aby przejścia były płynne
            AnimatedContent(targetState = title) { targetTitle ->
                Text(
                    text = targetTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // W razie gdyby tytuł był za długi
                )
            }
        },
        navigationIcon = {
            AnimatedVisibility(visible = showBackButton) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = T.get("navigation.back")
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSettingsClicked) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = T.get("settings.title")
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}