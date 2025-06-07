package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DoubleValidation
import org.octavius.novels.form.control.validation.DoubleValidator

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

        LaunchedEffect(controlState.value.value) {
            if (!controlState.dirty.value) {
                textValue = controlState.value.value?.toString() ?: ""
            }
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                controlState.dirty.value = true

                when {
                    newText.isEmpty() -> {
                        controlState.value.value = null
                    }

                    newText.matches(Regex("^\\d*[.,]?\\d*$")) -> {
                        val cleanText = newText.replace(",", ".")
                        cleanText.toDoubleOrNull()?.let { doubleValue ->
                            if (doubleValue.isFinite()) {
                                controlState.value.value = doubleValue
                            }
                        }
                    }

                    else -> {
                        // Błędy parsowania będą obsłużone przez walidację
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}