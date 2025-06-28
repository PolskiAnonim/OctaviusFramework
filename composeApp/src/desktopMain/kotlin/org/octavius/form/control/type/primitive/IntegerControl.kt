package org.octavius.form.control.type.primitive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.octavius.form.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.IntegerValidation
import org.octavius.form.control.validator.primitive.IntegerValidator
import org.octavius.ui.theme.FormSpacing

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
    override fun Display(controlName: String, controlState: ControlState<Int>, isRequired: Boolean) {
        var textValue by remember {
            mutableStateOf(controlState.value.value?.toString() ?: "")
        }
        var hasInvalidInput by remember { mutableStateOf(false) }

        LaunchedEffect(hasInvalidInput) {
            if (hasInvalidInput) {
                errorManager.setFieldErrors(controlName, listOf("Nieprawidłowy format liczby"))
            } else {
                errorManager.setFieldErrors(controlName, listOf())
            }
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                controlState.dirty.value = true

                if (newText.isEmpty()) {
                    controlState.value.value = null
                    hasInvalidInput = false
                } else {
                    try {
                        controlState.value.value = newText.toInt()
                        hasInvalidInput = false
                    } catch (e: NumberFormatException) {
                        hasInvalidInput = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(
                vertical = FormSpacing.fieldPaddingVertical,
                horizontal = FormSpacing.fieldPaddingHorizontal
            ),
            singleLine = true
        )
    }
}