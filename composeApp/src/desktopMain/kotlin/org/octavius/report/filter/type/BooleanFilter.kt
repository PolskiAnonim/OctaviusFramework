package org.octavius.report.filter.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterData
import org.octavius.domain.NullHandling
import org.octavius.report.filter.Filter

class BooleanFilter(columnName: String, val falseText: String, val trueText: String) : Filter(columnName) {

    override fun constructWhereClause(filter: FilterData<*>): String {
        val filterData = filter as FilterData.BooleanData
        return when {
            // Ignoruj filtrowanie gdy wartość null i nullHandling == Ignore
            filterData.value.value == null && filterData.nullHandling.value == NullHandling.Ignore -> ""
            // Gdy wartość określona i ignorujemy null
            filterData.value.value != null && filterData.nullHandling.value == NullHandling.Ignore ->
                "$columnName = ${filterData.value.value}"
            // Gdy wartość null i wykluczamy/włączamy null
            filterData.value.value == null && filterData.nullHandling.value != NullHandling.Ignore -> {
                if (filterData.nullHandling.value == NullHandling.Exclude) return "$columnName IS NOT NULL"
                else "$columnName IS NULL"
            }
            // Gdy wartość określona i wykluczamy/włączamy null
            else -> {
                if (filterData.nullHandling.value == NullHandling.Include) return "($columnName = ${filterData.value.value} OR $columnName IS NULL)"
                else "$columnName = ${filterData.value.value}"
            }
        }
    }

    override fun createFilterData(): FilterData<*> {
        return FilterData.BooleanData()
    }

    @Composable
    override fun RenderFilter(
        currentFilter: FilterData<*>
    ) {
        val filterData = currentFilter as FilterData.BooleanData
        val filterValue = filterData.value

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
                        filterData.markDirty()
                    },
                    label = { Text(trueText) },
                    leadingIcon = if (filterValue.value == true) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )

                FilterChip(
                    selected = filterValue.value == false,
                    onClick = {
                        filterValue.value = if (filterValue.value == false) null else false
                        filterData.markDirty()
                    },
                    label = { Text(falseText) },
                    leadingIcon = if (filterValue.value == false) {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Opcje dla obsługi null
            NullHandlingPanel(filterData)
        }
    }
}