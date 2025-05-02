package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.column.ReportColumn

class BooleanColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val trueText: String = "Tak",
    private val falseText: String = "Nie",
    private val showIcon: Boolean = true
) : ReportColumn(name, header, width, sortable, filterable) {

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        val value = item[name] as? Boolean

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value != null) {
                if (showIcon) {
                    Icon(
                        imageVector = if (value)
                            androidx.compose.material.icons.Icons.Default.Check
                        else
                            androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = if (value) trueText else falseText,
                        tint = if (value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = if (value) trueText else falseText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>?,
        onFilterChanged: (FilterValue<*>?) -> Unit
    ) {
        if (!filtrable) return

        var filterValue by remember { mutableStateOf<Boolean?>(null) }

        @Suppress("UNCHECKED_CAST")
        val booleanFilter = currentFilter as? FilterValue.BooleanFilter

        // Inicjalizacja z aktualnego filtra
        LaunchedEffect(currentFilter) {
            filterValue = booleanFilter?.value
        }

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Filtruj według wartości",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterValue == true,
                    onClick = {
                        filterValue = if (filterValue == true) null else true
                        if (filterValue == null) {
                            onFilterChanged(null)
                        } else {
                            onFilterChanged(
                                FilterValue.BooleanFilter(
                                    value = filterValue,
                                    nullHandling = NullHandling.Exclude
                                )
                            )
                        }
                    },
                    label = { Text(trueText) },
                    leadingIcon = if (filterValue == true) {
                        { Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        ) }
                    } else null
                )

                FilterChip(
                    selected = filterValue == false,
                    onClick = {
                        filterValue = if (filterValue == false) null else false
                        if (filterValue == null) {
                            onFilterChanged(null)
                        } else {
                            onFilterChanged(
                                FilterValue.BooleanFilter(
                                    value = filterValue,
                                    nullHandling = NullHandling.Exclude
                                )
                            )
                        }
                    },
                    label = { Text(falseText) },
                    leadingIcon = if (filterValue == false) {
                        { Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        ) }
                    } else null
                )
            }
        }
    }
}
