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
import org.octavius.novels.report.*
import org.octavius.novels.report.column.ReportColumn

class IntegerColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (Int?) -> String = { it?.toString() ?: "" }
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        return ColumnState(
            mutableStateOf(SortDirection.UNSPECIFIED),
            filtering = if (filterable) mutableStateOf(FilterValue.NumberFilter<Int>()) else mutableStateOf(
                null
            )
        )
    }

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val intFilter = filter as FilterValue.NumberFilter<Int>

        // Jeśli nie mamy wartości do filtrowania i ignorujemy nulle, zwróć pusty string
        if ((intFilter.minValue.value == null && intFilter.maxValue.value == null) &&
            intFilter.nullHandling.value == NullHandling.Ignore
        ) {
            return ""
        }

        // Budowanie podstawowej klauzuli w zależności od typu filtra
        val baseClause = when (intFilter.filterType.value) {
            NumberFilterType.Equals ->
                intFilter.minValue.value?.let { "$name = $it" } ?: ""

            NumberFilterType.NotEquals ->
                intFilter.minValue.value?.let { "$name <> $it" } ?: ""

            NumberFilterType.LessThan ->
                intFilter.minValue.value?.let { "$name < $it" } ?: ""

            NumberFilterType.LessEquals ->
                intFilter.minValue.value?.let { "$name <= $it" } ?: ""

            NumberFilterType.GreaterThan ->
                intFilter.minValue.value?.let { "$name > $it" } ?: ""

            NumberFilterType.GreaterEquals ->
                intFilter.minValue.value?.let { "$name >= $it" } ?: ""

            NumberFilterType.Range -> {
                val minCond = intFilter.minValue.value?.let { "$name >= $it" }
                val maxCond = intFilter.maxValue.value?.let { "$name <= $it" }

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
                NullHandling.Include -> "$name IS NULL"
                NullHandling.Exclude -> "$name IS NOT NULL"
                NullHandling.Ignore -> ""
            }
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (intFilter.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $name IS NULL)"
            NullHandling.Exclude -> "($baseClause AND $name IS NOT NULL)"
        }
    }

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
        currentFilter: FilterValue<*>
    ) {
        if (!filterable) return

        @Suppress("UNCHECKED_CAST")
        val numberFilter = currentFilter as FilterValue.NumberFilter<Int>
        val minValue = numberFilter.minValue
        val maxValue = numberFilter.maxValue
        val filterType = numberFilter.filterType
        val nullHandling = numberFilter.nullHandling

        var expanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when (filterType.value) {
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
                                expanded = false
                                filterType.value = type
                                numberFilter.markDirty()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filterType.value == NumberFilterType.Range) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minValue.value?.toString() ?: "",
                        onValueChange = {
                            try {
                                minValue.value = if (it.isEmpty()) null else it.toInt()
                                numberFilter.markDirty()
                            } catch (e: NumberFormatException) {
                                // Ignoruj niepoprawne wartości
                            }
                        },
                        label = { Text("Od") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = maxValue.value?.toString() ?: "",
                        onValueChange = {
                            try {
                                maxValue.value = if (it.isEmpty()) null else it.toInt()
                                numberFilter.markDirty()
                            } catch (e: NumberFormatException) {
                                // Ignoruj niepoprawne wartości
                            }
                        },
                        label = { Text("Do") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = minValue.value?.toString() ?: "",
                    onValueChange = {
                        try {
                            minValue.value = if (it.isEmpty()) null else it.toInt()
                            numberFilter.markDirty()
                        } catch (e: NumberFormatException) {
                            // Ignoruj niepoprawne wartości
                        }
                    },
                    label = { Text("Wartość") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (minValue.value.toString().isNotEmpty()) {
                            IconButton(onClick = {
                                minValue.value = null
                                numberFilter.markDirty()
                            }) {
                                Icon(Icons.Default.Clear, "Wyczyść filtr")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    onClick = {
                        nullHandling.value = NullHandling.Ignore
                        numberFilter.markDirty()
                    }
                )
                Text("Ignoruj", modifier = Modifier.padding(end = 12.dp))

                RadioButton(
                    selected = nullHandling.value == NullHandling.Include,
                    onClick = {
                        nullHandling.value = NullHandling.Include
                        numberFilter.markDirty()
                    }
                )
                Text("Dołącz", modifier = Modifier.padding(end = 12.dp))

                RadioButton(
                    selected = nullHandling.value == NullHandling.Exclude,
                    onClick = {
                        nullHandling.value = NullHandling.Exclude
                        numberFilter.markDirty()
                    }
                )
                Text("Wyklucz")
            }
        }
    }
}