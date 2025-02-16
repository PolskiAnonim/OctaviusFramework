package org.octavius.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.navigator.Screen

object MainScreen {

    var navigator: Navigator = Navigator()

    init {
        navigator.AddScreen(NovelMainScreen())
    }

    @Composable
    fun Content() {
        Box(Modifier.fillMaxSize()) {
            navigator.DisplayLast()
        }
    }
}