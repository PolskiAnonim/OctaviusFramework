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
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

/**
 * Kontrolka do wprowadzania liczb rzeczywistych (zmiennoprzecinkowych).
 *
 * Renderuje pole tekstowe z numeryczną klawiaturą i walidacją danych wejściowych.
 * Automatycznie sprawdza czy wprowadzona wartość jest poprawną liczbą rzeczywistą
 * i wyświetla komunikat błędu w przypadku nieprawidłowych danych.
 */
class DoubleControl(
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Double>(
    label,
    columnInfo,
    required,
    dependencies
) {
    override val validator: ControlValidator<Double> = DefaultValidator()

    @Composable
    override fun Display(
        controlState: ControlState<Double>,
        isRequired: Boolean
    ) {
        val textValue = controlState.value.value?.toString().orEmpty()

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {

            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    try {
                        if (newValue.isEmpty()) {
                            controlState.value.value = null
                        } else {
                            controlState.value.value = newValue.toDouble()
                        }
                        controlState.error.value = null
                    } catch (e: NumberFormatException) {
                        controlState.error.value = "Wartość musi być liczbą rzeczywistą"
                    }
                    controlState.dirty.value = true
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = controlState.error.value != null,
                singleLine = true
            )

        }
    }
}