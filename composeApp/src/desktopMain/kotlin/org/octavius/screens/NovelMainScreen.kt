package org.octavius.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.navigator.Screen

class NovelMainScreen: Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),contentAlignment = Alignment.Center) {
            Button(onClick = { navigator.AddScreen(NovelListScreen) }) {
                Text("Novels")
            }
        }
    }
}