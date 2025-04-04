package org.octavius.novels.form

import org.octavius.novels.form.control.Control

data class FormControls(
    val controls: Map<String, Control<*>>,
    val order: List<String>,
)

data class ControlResultData(
    val value: Any?,
    val dirty: Boolean
)