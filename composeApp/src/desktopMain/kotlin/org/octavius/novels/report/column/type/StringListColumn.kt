package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.TextFilterType
import org.octavius.novels.report.column.ReportColumn

class StringListColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val maxVisibleItems: Int = 3,
    private val separator: String? = null
) : ReportColumn(name, header, width, sortable, filterable) {

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item[name] as? List<String>

        if (value.isNullOrEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (separator != null) {
            // Wyświetl jako tekst z separatorem
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value.joinToString(separator),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Wyświetl jako listę elementów
            androidx.compose.foundation.layout.Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                val displayItems = if (value.size > maxVisibleItems) {
                    value.take(maxVisibleItems) + "... (${value.size - maxVisibleItems} więcej)"
                } else {
                    value
                }

                displayItems.forEach { item ->
                    SelectionContainer {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>?,
        onFilterChanged: (FilterValue<*>?) -> Unit
    ) {
        if (!filtrable) return

        var expanded by remember { mutableStateOf(false) }
        var filterText by remember { mutableStateOf("") }
        var filterType by remember { mutableStateOf(TextFilterType.Contains) }

        @Suppress("UNCHECKED_CAST")
        val textFilter = currentFilter as? FilterValue.TextFilter

        // Inicjalizacja wartości z aktualnego filtra
        LaunchedEffect(currentFilter) {
            textFilter?.let {
                filterText = it.value
                filterType = it.filterType
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Filtruj zawartość listy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = filterText,
                onValueChange = {
                    filterText = it
                    if (it.isNotEmpty()) {
                        onFilterChanged(
                            FilterValue.TextFilter(
                                filterType = filterType,
                                value = it,
                                nullHandling = NullHandling.Exclude
                            )
                        )
                    } else {
                        onFilterChanged(null)
                    }
                },
                label = { Text("Szukaj w liście") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = {
                            filterText = ""
                            onFilterChanged(null)
                        }) {
                            Icon(Icons.Default.Clear, "Wyczyść filtr")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when(filterType) {
                        TextFilterType.Exact -> "Element dokładnie równy"
                        TextFilterType.StartsWith -> "Element zaczyna się od"
                        TextFilterType.EndsWith -> "Element kończy się na"
                        TextFilterType.Contains -> "Element zawiera"
                        TextFilterType.NotContains -> "Element nie zawiera"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(
                        TextFilterType.Contains to "Element zawiera",
                        TextFilterType.StartsWith to "Element zaczyna się od",
                        TextFilterType.EndsWith to "Element kończy się na",
                        TextFilterType.Exact to "Element dokładnie równy",
                        TextFilterType.NotContains to "Element nie zawiera"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                filterType = type
                                expanded = false
                                if (filterText.isNotEmpty()) {
                                    onFilterChanged(
                                        FilterValue.TextFilter(
                                            filterType = type,
                                            value = filterText,
                                            nullHandling = NullHandling.Exclude
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Text(
                text = "Wskazówka: Ten filtr wyszukuje tekst w każdym elemencie listy.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}