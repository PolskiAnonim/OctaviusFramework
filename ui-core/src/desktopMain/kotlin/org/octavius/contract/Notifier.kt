package org.octavius.contract

import androidx.compose.runtime.staticCompositionLocalOf

interface Notifier {
    fun showMessage(message: String)
}

val LocalNotifier = staticCompositionLocalOf<Notifier> {
    error("No Notifier provided!")
}