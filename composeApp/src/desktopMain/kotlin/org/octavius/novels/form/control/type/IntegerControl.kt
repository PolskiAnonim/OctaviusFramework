package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
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
        isRequired: Boolean
    ) {
        val textValue = controlState.value.value?.toString().orEmpty()

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                try {
                    if (newValue.isEmpty()) {
                        controlState.value.value = null
                    } else {
                        controlState.value.value = newValue.toInt()
                    }
                    controlState.error.value = null
                } catch (e: NumberFormatException) {
                    controlState.error.value = "Wartość musi być liczbą całkowitą"
                }
                controlState.dirty.value = true
            },
            modifier = Modifier.fillMaxWidth(),
            isError = controlState.error.value != null,
            singleLine = true
        )
    }
}