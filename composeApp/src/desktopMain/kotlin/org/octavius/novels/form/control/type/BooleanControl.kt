package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.ControlState

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
    override fun display(controlName: String, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val state = states[controlName] as ControlState<Boolean>
        state.let { ctrlState ->
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
                    Checkbox(
                        checked = ctrlState.value.value ?: false,
                        onCheckedChange = {
                            ctrlState.value.value = it
                            ctrlState.dirty.value = true
                            ctrlState.touched.value = true
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