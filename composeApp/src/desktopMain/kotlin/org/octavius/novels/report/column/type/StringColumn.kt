package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.*
import org.octavius.novels.report.column.ReportColumn

class StringColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (String?) -> String = { it ?: "" }
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        return ColumnState(
            mutableStateOf(SortDirection.UNSPECIFIED),
            filtering = if (filterable) mutableStateOf(FilterValue.TextFilter()) else mutableStateOf(
                null
            )
        )
    }

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val textFilter = filter as FilterValue.TextFilter

        // Gdy nie mamy wartości do filtrowania
        if (textFilter.value.value.isEmpty() && textFilter.nullHandling.value == NullHandling.Ignore) {
            return ""
        }

        // Escape wartości tekstu dla SQL
        val escapedValue = textFilter.value.value.replace("'", "''")

        // Określenie czy wyszukiwanie powinno być case-sensitive
        val columnExpr = if (textFilter.caseSensitive.value) name else "LOWER($name)"
        val valueExpr = if (textFilter.caseSensitive.value) "'$escapedValue'" else "LOWER('$escapedValue')"

        // Budowanie podstawowej klauzuli w zależności od typu filtra
        val baseClause = when (textFilter.filterType.value) {
            TextFilterType.Exact ->
                "$columnExpr = $valueExpr"

            TextFilterType.StartsWith ->
                "$columnExpr LIKE '${escapedValue}%'"

            TextFilterType.EndsWith ->
                "$columnExpr LIKE '%${escapedValue}'"

            TextFilterType.Contains ->
                "$columnExpr LIKE '%${escapedValue}%'"

            TextFilterType.NotContains ->
                "$columnExpr NOT LIKE '%${escapedValue}%'"
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (textFilter.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $name IS NULL)"
            NullHandling.Exclude -> baseClause  // operatory LIKE/= już wykluczają NULL
        }
    }

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        val value = item[name] as? String

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>
    ) {
        if (!filterable) return

        val textFilter = currentFilter as? FilterValue.TextFilter ?: return
        val filterText = textFilter.value
        val filterType = textFilter.filterType
        val caseSensitive = textFilter.caseSensitive
        val nullHandling = textFilter.nullHandling

        Column(modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = filterText.value,
                onValueChange = {
                    filterText.value = it
                    // Filtr jest już aktualizowany poprzez MutableState
                },
                label = { Text("Filtruj") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (filterText.value.isNotEmpty()) {
                        IconButton(onClick = {
                            filterText.value = ""
                        }) {
                            Icon(Icons.Default.Clear, "Wyczyść filtr")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when(filterType.value) {
                        TextFilterType.Exact -> "Dokładnie"
                        TextFilterType.StartsWith -> "Zaczyna się od"
                        TextFilterType.EndsWith -> "Kończy się na"
                        TextFilterType.Contains -> "Zawiera"
                        TextFilterType.NotContains -> "Nie zawiera"
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
                        TextFilterType.Contains to "Zawiera",
                        TextFilterType.StartsWith to "Zaczyna się od",
                        TextFilterType.EndsWith to "Kończy się na",
                        TextFilterType.Exact to "Dokładnie",
                        TextFilterType.NotContains to "Nie zawiera"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                filterType.value = type
                                expanded = false
                            },
                            trailingIcon = if (filterType.value == type) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
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

            // Opcja dla wielkości liter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = caseSensitive.value,
                    onCheckedChange = { caseSensitive.value = it }
                )
                Text(
                    text = "Uwzględnij wielkość liter",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}