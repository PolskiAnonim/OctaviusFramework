package org.octavius.form.control.type.primitive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import org.octavius.data.contract.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.layout.RenderCheckboxLabel
import org.octavius.ui.theme.FormSpacing

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
    dependencies: Map<String, ControlDependency<*>>? = null,
    actions: List<ControlAction<Boolean>>? = null,
) : Control<Boolean>(
    label,
    columnInfo,
    required,
    dependencies,
    hasStandardLayout = false,
    actions = actions
) {
    @Composable
    override fun Display(controlName: String, controlState: ControlState<Boolean>, isRequired: Boolean) {
        val scope = rememberCoroutineScope()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FormSpacing.booleanControlPadding),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = FormSpacing.booleanRowPaddingVertical,
                            horizontal = FormSpacing.booleanRowPaddingHorizontal
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRequired) {
                        val currentValue = controlState.value.value ?: false
                        controlState.value.value = currentValue
                        Checkbox(
                            checked = currentValue,
                            onCheckedChange = {
                                controlState.value.value = it
                                executeActions(controlName, it, scope)
                            }
                        )
                    } else {
                        TriStateCheckbox(
                            state = controlState.value.value?.let { ToggleableState(it) }
                                ?: ToggleableState.Indeterminate,
                            onClick = {
                                val mapping = mapOf(null to false, false to true, true to null)
                                val newValue = mapping[controlState.value.value]
                                controlState.value.value = newValue
                                executeActions(controlName, newValue, scope)
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