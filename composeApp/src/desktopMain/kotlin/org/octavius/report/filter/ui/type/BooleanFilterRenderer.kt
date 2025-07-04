package org.octavius.report.filter.ui.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.filter.data.type.BooleanFilterData
import org.octavius.report.filter.ui.FilterColumnWrapper
import org.octavius.report.filter.ui.FilterModePanel
import org.octavius.report.filter.ui.FilterSpacer
import org.octavius.report.filter.ui.NullHandlingPanel

@Composable
fun BooleanFilterRenderer(filterData: BooleanFilterData, trueText: String, falseText: String) {
    FilterColumnWrapper {
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
                selected = filterData.value.value == true,
                onClick = {
                    filterData.value.value = if (filterData.value.value == true) null else true
                },
                label = { Text(trueText) },
                leadingIcon = if (filterData.value.value == true) {
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
                selected = filterData.value.value == false,
                onClick = {
                    filterData.value.value = if (filterData.value.value == false) null else false
                },
                label = { Text(falseText) },
                leadingIcon = if (filterData.value.value == false) {
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

        FilterSpacer()
        FilterModePanel(filterData)
        FilterSpacer()
        NullHandlingPanel(filterData)
    }
}

