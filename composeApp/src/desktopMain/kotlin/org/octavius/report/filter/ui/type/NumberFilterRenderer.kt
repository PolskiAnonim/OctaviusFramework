package org.octavius.report.filter.ui.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.domain.NumberFilterDataType
import org.octavius.localization.Translations
import org.octavius.report.filter.data.type.NumberFilterData
import org.octavius.report.filter.ui.EnumDropdownMenu
import org.octavius.report.filter.ui.FilterColumnWrapper
import org.octavius.report.filter.ui.FilterModePanel
import org.octavius.report.filter.ui.FilterSpacer
import org.octavius.report.filter.ui.NullHandlingPanel

@Composable
fun <T : Number> NumberFilterRenderer(filterData: NumberFilterData<T>, valueParser: (String) -> T?) {
    FilterColumnWrapper {
        EnumDropdownMenu(
            currentValue = filterData.filterType.value,
            options = NumberFilterDataType.entries,
            onValueChange = { filterData.filterType.value = it }
        )

        FilterSpacer()

        if (filterData.filterType.value == NumberFilterDataType.Range) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterData.minValue.value?.toString() ?: "",
                    onValueChange = { input ->
                        try {
                            filterData.minValue.value = if (input.isEmpty()) null else valueParser(input)
                        } catch (e: NumberFormatException) {
                            // Ignore invalid input
                            filterData.minValue.value = null
                        }
                    },
                    label = { Text(Translations.get("filter.number.from")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = filterData.maxValue.value?.toString() ?: "",
                    onValueChange = { input ->
                        try {
                            filterData.maxValue.value = if (input.isEmpty()) null else valueParser(input)
                        } catch (e: NumberFormatException) {
                            // Ignore invalid input
                        }
                    },
                    label = { Text(Translations.get("filter.number.to")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = filterData.minValue.value?.toString() ?: "",
                onValueChange = { input ->
                    try {
                        filterData.minValue.value = if (input.isEmpty()) null else valueParser(input)
                    } catch (e: NumberFormatException) {
                        // Ignore invalid input
                    }
                },
                label = { Text(Translations.get("filter.number.value")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (filterData.minValue.value != null) {
                        IconButton(onClick = { filterData.minValue.value = null }) {
                            Icon(Icons.Default.Clear, Translations.get("filter.general.clear"))
                        }
                    }
                }
            )
        }

        FilterSpacer()
        FilterModePanel(filterData)
        FilterSpacer()
        NullHandlingPanel(filterData)
    }
}

@Composable
fun IntegerFilterRenderer(filterData: NumberFilterData<Int>) {
    NumberFilterRenderer(filterData) { input -> input.toInt() }
}

@Composable
fun DoubleFilterRenderer(filterData: NumberFilterData<Double>) {
    NumberFilterRenderer(filterData) { input -> input.toDouble() }
}