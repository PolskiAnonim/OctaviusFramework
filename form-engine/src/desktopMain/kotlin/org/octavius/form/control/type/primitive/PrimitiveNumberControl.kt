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
import org.octavius.form.control.base.ValidationOptions
import org.octavius.ui.theme.FormSpacing

/**
 * Abstrakcyjna klasa bazowa dla kontrolek numerycznych (Integer, Double, etc.).
 * Hermetyzuje wspólną logikę renderowania pola tekstowego, parsowania
 * i obsługi błędów nieprawidłowego formatu.
 */
abstract class PrimitiveNumberControl<T : Number>(
    label: String?,
    columnInfo: ColumnInfo?,
    required: Boolean?,
    dependencies: Map<String, ControlDependency<*>>?,
    validationOptions: ValidationOptions?
) : Control<T>(
    label,
    columnInfo,
    required,
    dependencies,
    validationOptions = validationOptions
) {
    /**
     * Abstrakcyjna metoda, którą konkretne implementacje muszą dostarczyć,
     * aby sparsować tekst na docelowy typ numeryczny.
     * @param text Wartość z pola tekstowego.
     * @return Sparsowana wartość lub null w przypadku błędu formatu.
     */
    protected abstract fun parseValue(text: String): T?

    @Composable
    override fun Display(controlName: String, controlState: ControlState<T>, isRequired: Boolean) {
        var textValue by remember {
            mutableStateOf(controlState.value.value?.toString() ?: "")
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText

                if (newText.isEmpty()) {
                    controlState.value.value = null
                    errorManager.setFormatError(controlName,null)  // Wyczyść błąd formatu
                } else {
                    val parsed = parseValue(newText)
                    if (parsed != null) {
                        controlState.value.value = parsed
                        errorManager.setFormatError(controlName,null) // Wyczyść błąd formatu
                    } else {
                        errorManager.setFormatError(controlName,"Nieprawidłowy format liczby")
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