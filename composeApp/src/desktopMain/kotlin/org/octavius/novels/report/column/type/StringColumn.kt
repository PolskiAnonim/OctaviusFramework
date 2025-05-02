package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

class StringColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (String?) -> String = { it ?: "" }
) : ReportColumn(name, header, width, sortable, filterable) {

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        val value = item[name] as? String

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium
            )
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

        val textFilter = currentFilter as? FilterValue.TextFilter

        // Inicjalizacja wartości z aktualnego filtra
        LaunchedEffect(currentFilter) {
            textFilter?.let {
                filterText = it.value
                filterType = it.filterType
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
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
                label = { Text("Filtruj") },
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

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when(filterType) {
                        TextFilterType.Exact -> "Dokładnie"
                        TextFilterType.StartsWith -> "Zaczyna się od"
                        TextFilterType.EndsWith -> "Kończy się na"
                        TextFilterType.Contains -> "Zawiera"
                        TextFilterType.NotContains -> "Nie zawiera"
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
                        TextFilterType.Contains to "Zawiera",
                        TextFilterType.StartsWith to "Zaczyna się od",
                        TextFilterType.EndsWith to "Kończy się na",
                        TextFilterType.Exact to "Dokładnie",
                        TextFilterType.NotContains to "Nie zawiera"
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
                            },
                            trailingIcon = if (filterType == type) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}