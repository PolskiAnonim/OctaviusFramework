package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.domain.EnumWithFormatter
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.RenderError
import org.octavius.novels.form.control.RenderNormalLabel
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator
import kotlin.reflect.KClass

class EnumControl<T>(
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
) where T : Enum<T>, T : EnumWithFormatter<T> {
    override val validator: ControlValidator<T> = DefaultValidator()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Display(
        controlState: ControlState<T>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        controlState!!.let { ctrlState ->
            var expanded by remember { mutableStateOf(false) }
            val options = enumClass.java.enumConstants

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                RenderNormalLabel(label, isRequired)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pole z wybraną wartością
                    OutlinedTextField(
                        value = ctrlState.value.value?.toDisplayString()
                            ?: (if (!isRequired) "Brak wyboru" else "Wybierz opcję"),
                        onValueChange = { },  // Nie pozwalamy na edycję ręczną
                        readOnly = true,      // Pole tylko do odczytu
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                text = { Text(enumOption.toDisplayString()) },
                                onClick = {
                                    ctrlState.value.value = enumOption
                                    updateState(ctrlState)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                RenderError(ctrlState)
            }
        }
    }
}