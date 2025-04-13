package org.octavius.novels.form.control.type

import androidx.compose.runtime.Composable
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

class HiddenControl<T : Any>(fieldName: String?, tableName: String?) : Control<T>(
    label = null,
    fieldName,
    tableName,
    hidden = null,
    required = null,
    dependencies = null
) {
    override val validator: ControlValidator<T> = DefaultValidator()


    @Composable
    override fun display(
        controlState: ControlState<T>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ) {
        //Brak widocznej zawartości
    }
}