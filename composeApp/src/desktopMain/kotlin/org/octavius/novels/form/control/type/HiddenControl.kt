package org.octavius.novels.form.control.type

import androidx.compose.runtime.Composable
import org.octavius.novels.form.control.Control

class HiddenControl<T : Any>(fieldName: String?, tableName: String?) : Control<T>(
    state = null,
    label = null,
    fieldName,
    tableName,
    hidden = null,
    required = null,
    dependencies = null
) {
    @Composable
    override fun display(controls: Map<String, Control<*>>) {
        //Brak widocznej zawartości
    }
}