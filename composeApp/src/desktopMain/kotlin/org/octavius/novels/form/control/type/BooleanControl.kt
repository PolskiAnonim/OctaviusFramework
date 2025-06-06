package org.octavius.novels.form.control.type

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
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.RenderCheckboxLabel
import org.octavius.novels.form.control.RenderError
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

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
    dependencies
) {
    override val validator: ControlValidator<Boolean> = DefaultValidator()

    @Composable
    override fun Display(
        controlState: ControlState<Boolean>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {


        controlState.let { ctrlState ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRequired) {
                            ctrlState.value.value = ctrlState.value.value ?: false
                            Checkbox(
                                checked = ctrlState.value.value!!, // domyślnie false
                                onCheckedChange = {
                                    ctrlState.value.value = it
                                    updateState(ctrlState)
                                }
                            )
                        } else {
                            TriStateCheckbox(
                                state = if (ctrlState.value.value != null) ToggleableState(ctrlState.value.value!!) else ToggleableState.Indeterminate,
                                onClick = {
                                    val mapping = mapOf(null to false, false to true, true to null)
                                    ctrlState.value.value = mapping[ctrlState.value.value]
                                    updateState(ctrlState)
                                }
                            )
                        }
                        RenderCheckboxLabel(label, isRequired)
                    }

                    RenderError(ctrlState)
                }
            }
        }
    }


}