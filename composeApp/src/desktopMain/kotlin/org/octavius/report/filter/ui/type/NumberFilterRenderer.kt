package org.octavius.report.filter.ui.type

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.NumberFilterDataType
import org.octavius.localization.Translations
import org.octavius.report.filter.data.type.NumberFilterData
import org.octavius.report.filter.ui.*

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