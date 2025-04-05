package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency

class BooleanControl(
    fieldName: String?,
    tableName: String?,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Boolean>(
    label,
    fieldName,
    tableName,
    hidden,
    required,
    dependencies
) {
    @Composable
    override fun display(controlState: ControlState<Boolean>?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        controlState!!.let { ctrlState ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TriStateCheckbox(
                        state = if (ctrlState.value.value != null) ToggleableState(ctrlState.value.value!!) else ToggleableState.Indeterminate,
                        onClick = {
                            val mapping = mapOf(null to false,false to true,true to null)
                            ctrlState.value.value = mapping[ctrlState.value.value]
                            updateState(ctrlState)
                        }
                    )

                    Text(
                        text = label ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}