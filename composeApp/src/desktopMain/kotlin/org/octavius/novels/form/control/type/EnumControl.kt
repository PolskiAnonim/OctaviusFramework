package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator
import kotlin.reflect.KClass

class EnumControl<T: Enum<*>>(
    columnInfo: ColumnInfo?,
    label: String?,
    private val enumClass: KClass<T>,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<T>(
    label,
    columnInfo,
    hidden,
    required,
    dependencies
) {
    override val validator: ControlValidator<T> = DefaultValidator()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Display(
        controlState: ControlState<T>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ) {
        controlState!!.let { ctrlState ->
            var expanded by remember { mutableStateOf(false) }
            val options = enumClass.java.enumConstants

            // Sprawdzamy, czy kontrolka jest wymagana
            val isRequired = validator.isControlRequired(this, controls, states)

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                // Label z gwiazdką jeśli pole jest wymagane
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = label ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (isRequired) {
                        Text(
                            text = " *",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pole z wybraną wartością
                    OutlinedTextField(
                        value = ctrlState.value.value?.let { value ->
                            enumClass.members.firstOrNull { it.name == "toDisplayString" }
                                ?.call(value) as String
                        } ?: (if (!isRequired) "Brak wyboru" else "Wybierz opcję"),
                        onValueChange = { },  // Nie pozwalamy na edycję ręczną
                        readOnly = true,      // Pole tylko do odczytu
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)     // Oznacza ten element jako kotwicę dla menu
                    )

                    // Menu z opcjami
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        // Opcja null tylko jeśli kontrolka nie jest wymagana
                        if (!isRequired) {
                            DropdownMenuItem(
                                text = { Text("Brak wyboru") },
                                onClick = {
                                    ctrlState.value.value = null
                                    updateState(ctrlState)
                                    expanded = false
                                }
                            )

                            HorizontalDivider()
                        }

                        // Standardowe opcje enum
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

                // Komunikat o błędzie
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