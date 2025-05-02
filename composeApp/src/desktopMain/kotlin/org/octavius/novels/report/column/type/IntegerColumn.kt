package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.NumberFilterType
import org.octavius.novels.report.column.ReportColumn

class IntegerColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (Int?) -> String = { it?.toString() ?: "" }
) : ReportColumn(name, header, width, sortable, filterable) {

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        val value = item[name] as? Int

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
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
        var minValue by remember { mutableStateOf("") }
        var maxValue by remember { mutableStateOf("") }
        var filterType by remember { mutableStateOf(NumberFilterType.Equals) }

        @Suppress("UNCHECKED_CAST")
        val numberFilter = currentFilter as? FilterValue.NumberFilter<Int>

        // Inicjalizacja wartości z aktualnego filtra
        LaunchedEffect(currentFilter) {
            numberFilter?.let {
                minValue = it.minValue?.toString() ?: ""
                maxValue = it.maxValue?.toString() ?: ""
                filterType = it.filterType
            }
        }

        fun updateFilter() {
            val min = minValue.toIntOrNull()
            val max = maxValue.toIntOrNull()

            if ((filterType == NumberFilterType.Range && (min != null || max != null)) ||
                (filterType != NumberFilterType.Range && min != null)) {
                onFilterChanged(
                    FilterValue.NumberFilter(
                        filterType = filterType,
                        minValue = min,
                        maxValue = max,
                        nullHandling = NullHandling.Exclude
                    )
                )
            } else {
                onFilterChanged(null)
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when(filterType) {
                        NumberFilterType.Equals -> "Równe"
                        NumberFilterType.NotEquals -> "Różne od"
                        NumberFilterType.LessThan -> "Mniejsze niż"
                        NumberFilterType.LessEquals -> "Mniejsze lub równe"
                        NumberFilterType.GreaterThan -> "Większe niż"
                        NumberFilterType.GreaterEquals -> "Większe lub równe"
                        NumberFilterType.Range -> "Zakres"
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
                        NumberFilterType.Equals to "Równe",
                        NumberFilterType.NotEquals to "Różne od",
                        NumberFilterType.LessThan to "Mniejsze niż",
                        NumberFilterType.LessEquals to "Mniejsze lub równe",
                        NumberFilterType.GreaterThan to "Większe niż",
                        NumberFilterType.GreaterEquals to "Większe lub równe",
                        NumberFilterType.Range to "Zakres"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                filterType = type
                                expanded = false
                                updateFilter()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filterType == NumberFilterType.Range) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minValue,
                        onValueChange = {
                            minValue = it
                            updateFilter()
                        },
                        label = { Text("Od") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxValue,
                        onValueChange = {
                            maxValue = it
                            updateFilter()
                        },
                        label = { Text("Do") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = minValue,
                    onValueChange = {
                        minValue = it
                        updateFilter()
                    },
                    label = { Text("Wartość") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (minValue.isNotEmpty()) {
                            IconButton(onClick = {
                                minValue = ""
                                updateFilter()
                            }) {
                                Icon(Icons.Default.Clear, "Wyczyść filtr")
                            }
                        }
                    }
                )
            }
        }


    }
}