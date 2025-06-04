package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.RenderError
import org.octavius.novels.form.control.RenderNormalLabel
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

/**
 * Kontrolka do wprowadzania tekstu jednoliniowego.
 * 
 * Renderuje standardowe pole tekstowe z etykietą i walidacją.
 * Wspiera wszystkie standardowe funkcje kontrolek jak zależności,
 * wymagalność i komunikaty błędów.
 */
class TextControl(
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<String>(
    label,
    columnInfo,
    required,
    dependencies
) {
    override val validator: ControlValidator<String> = DefaultValidator()


    @Composable
    override fun Display(
        controlState: ControlState<String>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        controlState.let { ctrlState ->
            Column(modifier = Modifier.fillMaxWidth()) {
                RenderNormalLabel(label, isRequired)

                OutlinedTextField(
                    value = ctrlState.value.value ?: "",
                    onValueChange = {
                        ctrlState.value.value = it
                        ctrlState.dirty.value = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                    singleLine = true
                )

                RenderError(ctrlState)
            }
        }
    }
}