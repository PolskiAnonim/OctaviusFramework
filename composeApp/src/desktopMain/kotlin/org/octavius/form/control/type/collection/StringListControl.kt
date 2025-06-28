package org.octavius.form.control.type.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.form.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.StringListValidation
import org.octavius.form.control.validator.collection.TextListValidator
import org.octavius.localization.Translations

/**
 * Kontrolka do wprowadzania i zarządzania listą tekstów.
 *
 * Umożliwia dodawanie, edytowanie i usuwanie elementów tekstowych z listy.
 * Każdy element może być indywidualnie edytowany. Obsługuje dodawanie nowych
 * elementów poprzez dedykowane pole oraz usuwanie istniejących elementów.
 */
class StringListControl(
    columnInfo: ColumnInfo,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: StringListValidation? = null
) : Control<List<String>>(
    label, columnInfo, required, dependencies, validationOptions = validationOptions
) {
    override val validator: ControlValidator<List<String>> = TextListValidator(validationOptions)

    override fun copyInitToValue(value: List<String>): List<String> {
        @Suppress("UNCHECKED_CAST") return value.map { it }.toList()
    }

    @Composable
    override fun Display(controlName: String, controlState: ControlState<List<String>>, isRequired: Boolean) {
        var currentList by remember { mutableStateOf(controlState.value.value ?: listOf()) }
        var newItemText by remember { mutableStateOf("") }

        val minItems = (validationOptions as? StringListValidation)?.minItems ?: 0
        val maxItems = (validationOptions as? StringListValidation)?.maxItems

        // Ensure minimum items
        LaunchedEffect(currentList.size) {
            if (currentList.size < minItems) {
                val itemsToAdd = minItems - currentList.size
                val updatedList = currentList.toMutableList()
                repeat(itemsToAdd) { updatedList.add("") }
                currentList = updatedList
                controlState.value.value = updatedList
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with add button - consistent with RepeatableHeader height (48dp)
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translations.getPlural("form.stringList.items", currentList.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val canAdd = maxItems == null || currentList.size < maxItems
                if (canAdd) {
                    OutlinedButton(
                        onClick = {
                            val updatedList = currentList.toMutableList()
                            updatedList.add("")
                            currentList = updatedList
                            controlState.value.value = updatedList
                            controlState.dirty.value = true
                        },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = Translations.get("action.add"),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Translations.get("action.add"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items list - consistent spacing with RepeatableControl (8.dp between items)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentList.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small index indicator - consistent with label padding (4.dp start)
                        Text(
                            text = "${index + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp).padding(start = 4.dp)
                        )

                        OutlinedTextField(
                            value = item,
                            onValueChange = { newValue ->
                                val updatedList = currentList.toMutableList()
                                updatedList[index] = newValue
                                currentList = updatedList
                                controlState.value.value = updatedList
                                controlState.dirty.value = true
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            singleLine = true,
                            placeholder = { 
                                Text(
                                    text = Translations.get("form.stringList.itemPlaceholder"),
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            }
                        )

                        val canDelete = currentList.size > minItems
                        if (canDelete) {
                            IconButton(
                                onClick = {
                                    val updatedList = currentList.toMutableList()
                                    updatedList.removeAt(index)
                                    currentList = updatedList
                                    controlState.value.value = updatedList
                                    controlState.dirty.value = true
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = Translations.get("action.remove"),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Keep space for alignment when delete button isn't shown
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}