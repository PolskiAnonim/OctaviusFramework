package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<T>(
    label,
    fieldName,
    tableName,
    hidden,
    required,
    dependencies
) {
    @Composable
    override fun display(
        controlState: ControlState<T>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ) {
        controlState!!.let { ctrlState ->
            var expanded by remember { mutableStateOf(false) }
            val options = enumClass.java.enumConstants

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    text = label ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Tekst wartości
                        Text(
                            text = ctrlState.value.value?.let { value ->
                                enumClass.members.firstOrNull { it.name == "toDisplayString" }
                                    ?.call(value) as String
                            } ?: "Wybierz opcję",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Menedżer wyskakującego menu
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            options.forEach { enumOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(enumClass.members.firstOrNull { it.name == "toDisplayString" }
                                            ?.call(enumOption) as String)
                                    },
                                    onClick = {
                                        ctrlState.value.value = enumOption
                                        updateState(ctrlState)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Przycisk rozwijania menu
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Wybierz opcję")
                }
            }
        }
    }
}