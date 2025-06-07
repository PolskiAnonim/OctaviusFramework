package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.IntegerValidation
import org.octavius.novels.form.control.validation.IntegerValidator

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
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: IntegerValidation? = null
) : Control<Int>(
    label,
    columnInfo,
    required,
    dependencies,
    validationOptions = validationOptions
) {
    override val validator: ControlValidator<Int> = IntegerValidator(validationOptions)


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
                } catch (e: NumberFormatException) {
                    // Błędy parsowania będą obsłużone przez walidację
                }
                controlState.dirty.value = true
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}