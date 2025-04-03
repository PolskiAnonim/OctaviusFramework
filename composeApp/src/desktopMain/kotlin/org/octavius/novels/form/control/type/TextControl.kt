package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.ControlState

class TextControl(
    fieldName: String?,
    tableName: String?,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<String>(
    label,
    fieldName,
    tableName,
    hidden,
    required,
    dependencies
) {
    @Composable
    override fun display(controlName: String, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val state = states[controlName] as ControlState<String>
        state.let { ctrlState ->
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ctrlState.value.value ?: "",
                    onValueChange = {
                        ctrlState.value.value = it
                        ctrlState.dirty.value = true
                        ctrlState.touched.value = true
                    },
                    label = { Text(label ?: "") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    isError = ctrlState.error.value != null,
                    singleLine = true
                )

                if (ctrlState.error.value != null) {
                    Text(
                        text = ctrlState.error.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }

}