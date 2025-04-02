package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.*
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
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.ControlState

class TextListControl(
    fieldName: String?,
    tableName: String?,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<List<String>>(
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
            var currentList by remember { mutableStateOf(ctrlState.value.value ?: listOf("")) }
            var newItemText by remember { mutableStateOf("") }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label ?: "",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                ) {
                    itemsIndexed(currentList) { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = { newValue ->
                                    val updatedList = currentList.toMutableList()
                                    updatedList[index] = newValue
                                    currentList = updatedList
                                    ctrlState.value.value = updatedList
                                    ctrlState.dirty.value = true
                                    ctrlState.touched.value = true
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            if (currentList.size > 1) {
                                IconButton(
                                    onClick = {
                                        val updatedList = currentList.toMutableList()
                                        updatedList.removeAt(index)
                                        currentList = updatedList
                                        ctrlState.value.value = updatedList
                                        ctrlState.dirty.value = true
                                        ctrlState.touched.value = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń"
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Dodaj nowy element") },
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                val updatedList = currentList.toMutableList()
                                updatedList.add(newItemText)
                                currentList = updatedList
                                ctrlState.value.value = updatedList
                                newItemText = ""
                                ctrlState.dirty.value = true
                                ctrlState.touched.value = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dodaj"
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

    override fun convertValue(value: Any): Any? {
        // Obsługa konwersji z tablicy SQL do listy String
        return when (value) {
            is Array<*> -> value.mapNotNull { it?.toString() }
            is List<*> -> value.mapNotNull { it?.toString() }
            else -> super.convertValue(value)
        }
    }
}