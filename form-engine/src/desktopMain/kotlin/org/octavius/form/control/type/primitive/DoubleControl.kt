package org.octavius.form.control.type.primitive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.octavius.database.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.DoubleValidation
import org.octavius.form.control.validator.primitive.DoubleValidator
import org.octavius.ui.theme.FormSpacing

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
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: DoubleValidation? = null
) : Control<Double>(
    label,
    columnInfo,
    required,
    dependencies,
    validationOptions = validationOptions
) {
    override val validator: ControlValidator<Double> = DoubleValidator(validationOptions)

    @Composable
    override fun Display(controlName: String, controlState: ControlState<Double>, isRequired: Boolean) {
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

                if (newText.isEmpty()) {
                    controlState.value.value = null
                    hasInvalidInput = false
                } else {
                    val cleanText = newText.replace(",", ".")
                    val doubleValue = cleanText.toDoubleOrNull()
                    if (doubleValue != null && doubleValue.isFinite()) {
                        controlState.value.value = doubleValue
                        hasInvalidInput = false
                    } else {
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