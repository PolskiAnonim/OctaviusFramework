package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.ControlState
import kotlin.reflect.KClass

class EnumControl<T: Enum<*>>(
    fieldName: String?,
    tableName: String?,
    label: String?,
    private val enumClass: KClass<T>,
    private val displayNameFn: (T) -> String = { it.name },
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<T>(
    ControlState(),
    label,
    fieldName,
    tableName,
    hidden,
    required,
    dependencies
) {
    @Composable
    override fun display(controls: Map<String, Control<*>>) {
        state?.let { ctrlState ->
            var expanded by remember { mutableStateOf(false) }
            val options = enumClass.java.enumConstants

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ctrlState.value.value?.let { value ->
                            enumClass.members.firstOrNull { it.name == "toDisplayString" }
                            ?.call(value) as String } ?: "Wybierz opcję"
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEach { enumOption ->
                        DropdownMenuItem(
                            onClick = {
                                ctrlState.value.value = enumOption
                                ctrlState.dirty.value = true
                                ctrlState.touched.value = true
                                expanded = false
                            },
                            text = { Text(enumClass.members.firstOrNull { it.name == "toDisplayString" }
                                ?.call(enumOption) as String) }
                        )
                    }
                }

                if (ctrlState.error.value != null) {
                    Text(
                        text = ctrlState.error.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}