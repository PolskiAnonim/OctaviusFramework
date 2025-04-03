package org.octavius.novels.form

import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlState

data class FormControls(
    val controls: Map<String, Control<*>>,
    val order: List<String>,
)