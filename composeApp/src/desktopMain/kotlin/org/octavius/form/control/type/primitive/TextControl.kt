package org.octavius.form.control.type.primitive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.form.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.TextValidation
import org.octavius.form.control.validator.primitive.TextValidator

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
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: TextValidation? = null
) : Control<String>(
    label,
    columnInfo,
    required,
    dependencies,
    validationOptions = validationOptions
) {
    override val validator: ControlValidator<String> = TextValidator(validationOptions)


    @Composable
    override fun Display(controlName: String, controlState: ControlState<String>, isRequired: Boolean) {
        OutlinedTextField(
            value = controlState.value.value.orEmpty(),
            onValueChange = {
                controlState.value.value = it
                controlState.dirty.value = true
            },
            modifier = Modifier.Companion.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            singleLine = true
        )
    }
}