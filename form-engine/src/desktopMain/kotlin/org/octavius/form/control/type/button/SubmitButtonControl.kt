package org.octavius.form.control.type.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.validator.DefaultValidator

/**
 * Specjalizowany przycisk do wywoływania głównych akcji formularza (Zapisz, Anuluj)
 * lub niestandardowych przepływów.
 *
 * Integruje się z cyklem życia FormHandlera poprzez interfejs FormSubmitter.
 *
 * @param text Tekst na przycisku.
 * @param buttonType Styl wizualny (FILLED, OUTLINED).
 * @param dependencies Zależności widoczności.
 * @param validate - czy walidować formularz
 */
class SubmitButtonControl(
    val text: String,
    val actionKey: String,
    val validates: Boolean = false,
    val buttonType: ButtonType = ButtonType.Filled,
    dependencies: Map<String, ControlDependency<*>>? = null,
) : Control<Unit>(
    label = null,
    columnInfo = null,
    required = false,
    dependencies = dependencies,
    hasStandardLayout = false
) {
    override val validator: ControlValidator<Unit> = DefaultValidator()

    @Composable
    override fun Display(controlName: String, controlState: ControlState<Unit>, isRequired: Boolean) {
        val onClick = {
            formActionTrigger.triggerAction(actionKey, validates)
        }

        when (buttonType) {
            ButtonType.Filled -> Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
            ButtonType.Outlined -> OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
            ButtonType.Text -> TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
        }
    }
}