package org.octavius.form.control.type.number.primitive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.octavius.form.control.base.*
import org.octavius.ui.theme.FormSpacing

/**
 * Abstrakcyjna klasa bazowa dla kontrolek numerycznych (Integer, Double, etc.).
 * Hermetyzuje wspólną logikę renderowania pola tekstowego, parsowania
 * i obsługi błędów nieprawidłowego formatu.
 */
abstract class PrimitiveNumberControl<T : Number>(
    label: String?,
    required: Boolean?,
    dependencies: Map<String, ControlDependency<*>>?,
    validationOptions: ValidationOptions?,
    actions: List<ControlAction<T>>?
) : Control<T>(
    label,
    required,
    dependencies,
    validationOptions = validationOptions,
    actions = actions
) {
    /**
     * Abstrakcyjna metoda, którą konkretne implementacje muszą dostarczyć,
     * aby sparsować tekst na docelowy typ numeryczny.
     * @param text Wartość z pola tekstowego.
     * @return Sparsowana wartość lub null w przypadku błędu formatu.
     */
    protected abstract fun parseValue(text: String): T?

    @Composable
    override fun Display(renderContext: RenderContext, controlState: ControlState<T>, isRequired: Boolean) {
        // Stan widoku, który jest synchronizowany z modelem.
        var textValue by remember { mutableStateOf(controlState.value.value?.toString() ?: "") }
        val scope = rememberCoroutineScope()

        // EFEKT SYNCHRONIZUJĄCY dla zmian z zewnątrz
        // Odpali się za każdym razem, gdy akcja wywoła `updateControl`.
        LaunchedEffect(controlState.revision.value) {
            // Zawsze, gdy dostajemy sygnał z zewnątrz, bezwarunkowo synchronizujemy stan
            // widoku (textValue) ze stanem modelu (controlState.value).
            // To kasuje błędne wpisy użytkownika i czyści błędy.
            textValue = controlState.value.value?.toString() ?: ""
            errorManager.setFormatError(renderContext.fullPath, null)
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText

                if (newText.isEmpty()) {
                    if (controlState.value.value != null) {
                        controlState.value.value = null
                        executeActions(renderContext, null, scope)
                    }
                    errorManager.setFormatError(renderContext.fullPath, null)
                } else {
                    val parsed = parseValue(newText)
                    if (parsed != null) {
                        if (controlState.value.value != parsed) {
                            controlState.value.value = parsed
                            executeActions(renderContext, parsed, scope)
                        }
                        errorManager.setFormatError(renderContext.fullPath, null)
                    } else {
                        errorManager.setFormatError(renderContext.fullPath, "Nieprawidłowy format liczby")
                    }
                }
            },
            modifier = Modifier.Companion.fillMaxWidth().padding(
                vertical = FormSpacing.fieldPaddingVertical,
                horizontal = FormSpacing.fieldPaddingHorizontal
            ),
            singleLine = true
        )
    }
}