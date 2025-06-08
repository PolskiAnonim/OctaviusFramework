package org.octavius.novels.form.control.type.primitive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.base.Control
import org.octavius.novels.form.control.base.ControlDependency
import org.octavius.novels.form.control.base.ControlValidator
import org.octavius.novels.form.control.base.IntegerValidation
import org.octavius.novels.form.control.validator.primitive.IntegerValidator

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
            modifier = Modifier.Companion.fillMaxWidth(),
            singleLine = true
        )
    }
}