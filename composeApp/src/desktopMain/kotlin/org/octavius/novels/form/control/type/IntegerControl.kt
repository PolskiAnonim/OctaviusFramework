package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.ControlState

class IntegerControl(
    fieldName: String?,
    tableName: String?,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Int>(
    label,
    fieldName,
    tableName,
    hidden,
    required,
    dependencies
) {
    @Composable
    override fun display(controlName: String, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val state = states[controlName] as ControlState<Int>
        state.let { ctrlState ->
            val textValue = if (ctrlState.value.value != null) ctrlState.value.value.toString() else ""

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = label ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        try {
                            if (newValue.isEmpty()) {
                                ctrlState.value.value = null
                            } else {
                                ctrlState.value.value = newValue.toInt()
                            }
                            ctrlState.error.value = null
                        } catch (e: NumberFormatException) {
                            ctrlState.error.value = "Wartość musi być liczbą całkowitą"
                        }
                        ctrlState.dirty.value = true
                        ctrlState.touched.value = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ctrlState.error.value != null,
                    singleLine = true
                )

                if (ctrlState.error.value != null) {
                    Text(
                        text = ctrlState.error.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}