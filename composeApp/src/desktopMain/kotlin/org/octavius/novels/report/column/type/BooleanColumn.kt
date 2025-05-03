package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.ColumnState
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.SortDirection
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
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        return ColumnState(
            mutableStateOf(SortDirection.UNSPECIFIED),
            filtering = if (filterable) mutableStateOf(FilterValue.BooleanFilter()) else mutableStateOf(null)
        )
    }

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val booleanFilter = filter as FilterValue.BooleanFilter
        return when {
            // Ignoruj filtrowanie gdy wartość null i nullHandling == Ignore
            booleanFilter.value.value == null && booleanFilter.nullHandling.value == NullHandling.Ignore -> ""
            // Gdy wartość określona i ignorujemy null
            booleanFilter.value.value != null && booleanFilter.nullHandling.value == NullHandling.Ignore ->
                "$name = ${booleanFilter.value.value}"
            // Gdy wartość null i wykluczamy/włączamy null
            booleanFilter.value.value == null && booleanFilter.nullHandling.value != NullHandling.Ignore -> {
                if (booleanFilter.nullHandling.value == NullHandling.Exclude) return "$name IS NOT NULL"
                else "$name IS NULL"
            }
            // Gdy wartość określona i wykluczamy/włączamy null
            else -> {
                if (booleanFilter.nullHandling.value == NullHandling.Include) return "($name = ${booleanFilter.value.value} OR $name IS NULL)"
                else "$name = ${booleanFilter.value.value}"
            }
        }
    }

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
        currentFilter: FilterValue<*>,
        onFilterChanged: (FilterValue<*>?) -> Unit
    ) {
        if (!filterable) return

        val booleanFilter = currentFilter as? FilterValue.BooleanFilter ?: return
        val filterValue = booleanFilter.value
        val nullHandling = booleanFilter.nullHandling

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
                    selected = filterValue.value == true,
                    onClick = {
                        filterValue.value = if (filterValue.value == true) null else true
                    },
                    label = { Text(trueText) },
                    leadingIcon = if (filterValue.value == true) {
                        { Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        ) }
                    } else null
                )

                FilterChip(
                    selected = filterValue.value == false,
                    onClick = {
                        filterValue.value = if (filterValue.value == false) null else false
                    },
                    label = { Text(falseText) },
                    leadingIcon = if (filterValue.value == false) {
                        { Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        ) }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opcje dla obsługi null
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wartości puste:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                RadioButton(
                    selected = nullHandling.value == NullHandling.Ignore,
                    onClick = { nullHandling.value = NullHandling.Ignore }
                )
                Text("Ignoruj", modifier = Modifier.padding(end = 12.dp))

                RadioButton(
                    selected = nullHandling.value == NullHandling.Include,
                    onClick = { nullHandling.value = NullHandling.Include }
                )
                Text("Dołącz", modifier = Modifier.padding(end = 12.dp))

                RadioButton(
                    selected = nullHandling.value == NullHandling.Exclude,
                    onClick = { nullHandling.value = NullHandling.Exclude }
                )
                Text("Wyklucz")
            }
        }
    }
}
