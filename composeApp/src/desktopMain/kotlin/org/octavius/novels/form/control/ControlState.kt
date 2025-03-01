package org.octavius.novels.form.control

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class ControlState<T> {
    val value: MutableState<T?> = mutableStateOf(null)
}