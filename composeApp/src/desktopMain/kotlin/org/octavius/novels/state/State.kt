package org.octavius.novels.state

import androidx.compose.runtime.compositionLocalOf

val LocalState = compositionLocalOf<State> { error("No State found!") }

object State {
}