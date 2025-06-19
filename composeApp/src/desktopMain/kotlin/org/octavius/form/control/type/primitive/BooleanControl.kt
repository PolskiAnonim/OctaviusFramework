package org.octavius.form.control.type.primitive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import org.octavius.form.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.layout.RenderCheckboxLabel
import org.octavius.form.control.validator.DefaultValidator

/**
 * Kontrolka do wprowadzania wartości logicznych (prawda/fałsz).
 *
 * Renderuje checkbox z etykietą. Dla pól wymaganych używa standardowego
 * checkboxa (true/false), dla opcjonalnych używa checkboxa z trzema stanami
 * (true/false/null). Obsługuje automatyczne przełączanie między stanami.
 */
class BooleanControl(
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Boolean>(
    label,
    columnInfo,
    required,
    dependencies,
    hasStandardLayout = false
) {
    override val validator: ControlValidator<Boolean> = DefaultValidator()

    @Composable
    override fun Display(controlName: String, controlState: ControlState<Boolean>, isRequired: Boolean) {
        Surface(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    if (isRequired) {
                        val currentValue = controlState.value.value ?: false
                        controlState.value.value = currentValue
                        Checkbox(
                            checked = currentValue,
                            onCheckedChange = {
                                controlState.value.value = it
                                updateState(controlState)
                            }
                        )
                    } else {
                        TriStateCheckbox(
                            state = controlState.value.value?.let { ToggleableState(it) }
                                ?: ToggleableState.Indeterminate,
                            onClick = {
                                val mapping = mapOf(null to false, false to true, true to null)
                                controlState.value.value = mapping[controlState.value.value]
                                updateState(controlState)
                            }
                        )
                    }
                    RenderCheckboxLabel(label, isRequired)
                }

                DisplayFieldErrors(controlName)
            }
        }
    }
}