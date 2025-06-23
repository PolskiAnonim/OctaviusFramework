package org.octavius.form.control.type.collection

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import org.octavius.form.control.base.TextListValidation
import org.octavius.form.control.validator.collection.TextListValidator
import org.octavius.localization.Translations

/**
 * Kontrolka do wprowadzania i zarządzania listą tekstów.
 *
 * Umożliwia dodawanie, edytowanie i usuwanie elementów tekstowych z listy.
 * Każdy element może być indywidualnie edytowany. Obsługuje dodawanie nowych
 * elementów poprzez dedykowane pole oraz usuwanie istniejących elementów.
 */
class TextListControl(
    columnInfo: ColumnInfo,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: TextListValidation? = null
) : Control<List<String>>(
    label, columnInfo, required, dependencies, validationOptions = validationOptions
) {
    override val validator: ControlValidator<List<String>> = TextListValidator(validationOptions)

    override fun copyInitToValue(value: List<String>): List<String> {
        @Suppress("UNCHECKED_CAST") return value.map { it }.toList()
    }

    @Composable
    override fun Display(controlName: String, controlState: ControlState<List<String>>, isRequired: Boolean) {

        var currentList by remember { mutableStateOf(controlState.value.value ?: listOf("")) }
        var newItemText by remember { mutableStateOf("") }

        LazyColumn(
            modifier = Modifier.Companion.fillMaxWidth().heightIn(max = 200.dp)
        ) {
            itemsIndexed(currentList) { index, item ->
                Row(
                    modifier = Modifier.Companion.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item, onValueChange = { newValue ->
                            val updatedList = currentList.toMutableList()
                            updatedList[index] = newValue
                            currentList = updatedList
                            controlState.value.value = updatedList
                            controlState.dirty.value = true
                        }, modifier = Modifier.Companion.weight(1f).padding(end = 8.dp), singleLine = true
                    )

                    if (currentList.size > 1) {
                        IconButton(
                            onClick = {
                                val updatedList = currentList.toMutableList()
                                updatedList.removeAt(index)
                                currentList = updatedList
                                controlState.value.value = updatedList
                                controlState.dirty.value = true
                            }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = Translations.get("action.remove"),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Pole do dodawania nowego tytułu
        Row(
            modifier = Modifier.Companion.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.Companion.weight(1f).padding(end = 8.dp),
                placeholder = { Text(Translations.get("form.actions.addNewItem")) },
                singleLine = true
            )

            FilledTonalButton(
                onClick = {
                    if (newItemText.isNotBlank()) {
                        val updatedList = currentList.toMutableList()
                        updatedList.add(newItemText.trim())
                        currentList = updatedList
                        controlState.value.value = updatedList
                        newItemText = ""
                        controlState.dirty.value = true
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Add, contentDescription = Translations.get("action.add")
                )
            }
        }
    }
}