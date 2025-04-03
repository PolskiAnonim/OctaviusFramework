package org.octavius.novels.form.control.type

import androidx.compose.runtime.Composable
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlState

class HiddenControl<T : Any>(fieldName: String?, tableName: String?) : Control<T>(
    label = null,
    fieldName,
    tableName,
    hidden = null,
    required = null,
    dependencies = null
) {
    @Composable
    override fun display(controlName: String, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        //Brak widocznej zawartości
    }
}