package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
 * Kontrolka do wprowadzania liczb całkowitych.
 * 
 * Renderuje pole tekstowe z numeryczną klawiaturą i walidacją danych wejściowych.
 * Automatycznie sprawdza czy wprowadzona wartość jest poprawną liczbą całkowitą
 * i wyświetla komunikat błędu w przypadku nieprawidłowych danych.
 */
class IntegerControl(
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Int>(
    label,
    columnInfo,
    required,
    dependencies
) {
    override val validator: ControlValidator<Int> = DefaultValidator()


    @Composable
    override fun Display(
        controlState: ControlState<Int>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        controlState.let { ctrlState ->
            val textValue = if (ctrlState.value.value != null) ctrlState.value.value.toString() else ""

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                RenderNormalLabel(label, isRequired)

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        try {
                            if (newValue.isEmpty()) {
                                ctrlState.value.value = null
                            } else {
                                ctrlState.value.value = newValue.toInt()
                            }
                            ctrlState.error.value = null
                        } catch (e: NumberFormatException) {
                            ctrlState.error.value = "Wartość musi być liczbą całkowitą"
                        }
                        ctrlState.dirty.value = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ctrlState.error.value != null,
                    singleLine = true
                )

                RenderError(ctrlState)
            }
        }
    }
}