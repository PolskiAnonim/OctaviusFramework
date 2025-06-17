package org.octavius.report.filter.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterData
import org.octavius.report.NullHandling
import org.octavius.report.NumberFilterDataType
import org.octavius.report.filter.Filter

class IntegerFilter(columnName: String) : Filter(columnName) {

    override fun constructWhereClause(filter: FilterData<*>): String {
        @Suppress("UNCHECKED_CAST")
        val intFilter = filter as FilterData.NumberData<Int>

        // Jeśli nie mamy wartości do filtrowania i ignorujemy nulle, zwróć pusty string
        if ((intFilter.minValue.value == null && intFilter.maxValue.value == null) &&
            intFilter.nullHandling.value == NullHandling.Ignore
        ) {
            return ""
        }

        // Budowanie podstawowej klauzuli w zależności od typu filtra
        val baseClause = when (intFilter.filterType.value) {
            NumberFilterDataType.Equals ->
                intFilter.minValue.value?.let { "$columnName = $it" } ?: ""

            NumberFilterDataType.NotEquals ->
                intFilter.minValue.value?.let { "$columnName <> $it" } ?: ""

            NumberFilterDataType.LessThan ->
                intFilter.minValue.value?.let { "$columnName < $it" } ?: ""

            NumberFilterDataType.LessEquals ->
                intFilter.minValue.value?.let { "$columnName <= $it" } ?: ""

            NumberFilterDataType.GreaterThan ->
                intFilter.minValue.value?.let { "$columnName > $it" } ?: ""

            NumberFilterDataType.GreaterEquals ->
                intFilter.minValue.value?.let { "$columnName >= $it" } ?: ""

            NumberFilterDataType.Range -> {
                val minCond = intFilter.minValue.value?.let { "$columnName >= $it" }
                val maxCond = intFilter.maxValue.value?.let { "$columnName <= $it" }

                when {
                    minCond != null && maxCond != null -> "($minCond AND $maxCond)"
                    minCond != null -> minCond
                    maxCond != null -> maxCond
                    else -> ""
                }
            }
        }

        // Jeśli nie mamy podstawowej klauzuli (brak wartości), obsłuż tylko nulle
        if (baseClause.isEmpty()) {
            return when (intFilter.nullHandling.value) {
                NullHandling.Include -> "$columnName IS NULL"
                NullHandling.Exclude -> "$columnName IS NOT NULL"
                NullHandling.Ignore -> ""
            }
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (intFilter.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $columnName IS NULL)"
            NullHandling.Exclude -> "($baseClause AND $columnName IS NOT NULL)"
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderFilter(
        currentFilter: FilterData<*>
    ) {
        @Suppress("UNCHECKED_CAST")
        val filterData = currentFilter as FilterData.NumberData<Int>
        val minValue = filterData.minValue
        val maxValue = filterData.maxValue
        val filterType = filterData.filterType

        var expanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when (filterType.value) {
                        NumberFilterDataType.Equals -> "Równe"
                        NumberFilterDataType.NotEquals -> "Różne od"
                        NumberFilterDataType.LessThan -> "Mniejsze niż"
                        NumberFilterDataType.LessEquals -> "Mniejsze lub równe"
                        NumberFilterDataType.GreaterThan -> "Większe niż"
                        NumberFilterDataType.GreaterEquals -> "Większe lub równe"
                        NumberFilterDataType.Range -> "Zakres"
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
                        NumberFilterDataType.Equals to "Równe",
                        NumberFilterDataType.NotEquals to "Różne od",
                        NumberFilterDataType.LessThan to "Mniejsze niż",
                        NumberFilterDataType.LessEquals to "Mniejsze lub równe",
                        NumberFilterDataType.GreaterThan to "Większe niż",
                        NumberFilterDataType.GreaterEquals to "Większe lub równe",
                        NumberFilterDataType.Range to "Zakres"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                filterType.value = type
                                filterData.markDirty()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filterType.value == NumberFilterDataType.Range) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minValue.value?.toString() ?: "",
                        onValueChange = {
                            try {
                                minValue.value = if (it.isEmpty()) null else it.toInt()
                                filterData.markDirty()
                            } catch (e: NumberFormatException) {
                                // Ignoruj niepoprawne wartości
                            }
                        },
                        label = { Text("Od") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxValue.value?.toString() ?: "",
                        onValueChange = {
                            try {
                                maxValue.value = if (it.isEmpty()) null else it.toInt()
                                filterData.markDirty()
                            } catch (e: NumberFormatException) {
                                // Ignoruj niepoprawne wartości
                            }
                        },
                        label = { Text("Do") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = minValue.value?.toString() ?: "",
                    onValueChange = {
                        try {
                            minValue.value = if (it.isEmpty()) null else it.toInt()
                            filterData.markDirty()
                        } catch (e: NumberFormatException) {
                            // Ignoruj niepoprawne wartości
                        }
                    },
                    label = { Text("Wartość") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (minValue.value.toString().isNotEmpty()) {
                            IconButton(onClick = {
                                minValue.value = null
                                filterData.markDirty()
                            }) {
                                Icon(Icons.Default.Clear, "Wyczyść filtr")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NullHandlingPanel(filterData)
        }
    }
}