package org.octavius.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.octavius.localization.Translations

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = Translations.get("app.name"),
        state = rememberWindowState(size = DpSize(1280.dp, 720.dp))
    ) {
        App()
    }
}