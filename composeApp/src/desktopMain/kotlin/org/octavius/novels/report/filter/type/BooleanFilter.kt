package org.octavius.novels.report.filter.type

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.filter.Filter

class BooleanFilter(name: String, val falseText: String, val trueText: String): Filter(name) {

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val booleanFilter = filter as FilterValue.BooleanFilter
        return when {
            // Ignoruj filtrowanie gdy wartość null i nullHandling == Ignore
            booleanFilter.value.value == null && booleanFilter.nullHandling.value == NullHandling.Ignore -> ""
            // Gdy wartość określona i ignorujemy null
            booleanFilter.value.value != null && booleanFilter.nullHandling.value == NullHandling.Ignore ->
                "$columnName = ${booleanFilter.value.value}"
            // Gdy wartość null i wykluczamy/włączamy null
            booleanFilter.value.value == null && booleanFilter.nullHandling.value != NullHandling.Ignore -> {
                if (booleanFilter.nullHandling.value == NullHandling.Exclude) return "$columnName IS NOT NULL"
                else "$columnName IS NULL"
            }
            // Gdy wartość określona i wykluczamy/włączamy null
            else -> {
                if (booleanFilter.nullHandling.value == NullHandling.Include) return "($columnName = ${booleanFilter.value.value} OR $columnName IS NULL)"
                else "$columnName = ${booleanFilter.value.value}"
            }
        }
    }

    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>
    ) {
        val booleanFilter = currentFilter as FilterValue.BooleanFilter
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
                        booleanFilter.markDirty()
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
                        booleanFilter.markDirty()
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
            NullHandlingPanel(booleanFilter)
        }
    }
}