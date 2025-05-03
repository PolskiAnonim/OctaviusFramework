package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.*
import org.octavius.novels.report.column.ReportColumn

class StringListColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val maxVisibleItems: Int = 3,
    private val separator: String? = null
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
        val valueExpr = if (textFilter.caseSensitive.value) "'$escapedValue'" else "LOWER('$escapedValue')"

        // Budowanie klauzuli dla listy tekstów
        // W przypadku listy musimy sprawdzić, czy jakikolwiek element spełnia warunek
        val baseClause = when (textFilter.filterType.value) {
            TextFilterType.Exact ->
                "$name @> ARRAY[$valueExpr]"  // Sprawdza czy lista zawiera dokładnie tę wartość

            TextFilterType.StartsWith ->
                "EXISTS (SELECT 1 FROM unnest($name) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '${escapedValue}%')"

            TextFilterType.EndsWith ->
                "EXISTS (SELECT 1 FROM unnest($name) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}')"

            TextFilterType.Contains ->
                "EXISTS (SELECT FROM unnest($name) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}%')"

            TextFilterType.NotContains ->
                "NOT EXISTS (SELECT FROM unnest($name) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}%')"
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (textFilter.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $name IS NULL)"
            NullHandling.Exclude -> "($baseClause AND $name IS NOT NULL)"
        }
    }

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item[name] as? List<String>

        if (value.isNullOrEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (separator != null) {
            // Wyświetl jako tekst z separatorem
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value.joinToString(separator),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Wyświetl jako listę elementów
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            ) {
                val displayItems = if (value.size > maxVisibleItems) {
                    value.take(maxVisibleItems) + "... (${value.size - maxVisibleItems} więcej)"
                } else {
                    value
                }

                displayItems.forEach { item ->
                    SelectionContainer {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>,
        onFilterChanged: (FilterValue<*>?) -> Unit
    ) {
        if (!filterable) return

        val textFilter = currentFilter as? FilterValue.TextFilter ?: return
        val filterText = textFilter.value
        val filterType = textFilter.filterType
        val nullHandling = textFilter.nullHandling

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Filtruj zawartość listy",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = filterText.value,
                onValueChange = {
                    filterText.value = it
                },
                label = { Text("Szukaj w liście") },
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
                        TextFilterType.Exact -> "Element dokładnie równy"
                        TextFilterType.StartsWith -> "Element zaczyna się od"
                        TextFilterType.EndsWith -> "Element kończy się na"
                        TextFilterType.Contains -> "Element zawiera"
                        TextFilterType.NotContains -> "Element nie zawiera"
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
                        TextFilterType.Contains to "Element zawiera",
                        TextFilterType.StartsWith to "Element zaczyna się od",
                        TextFilterType.EndsWith to "Element kończy się na",
                        TextFilterType.Exact to "Element dokładnie równy",
                        TextFilterType.NotContains to "Element nie zawiera"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                filterType.value = type
                                expanded = false
                            }
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

            Text(
                text = "Wskazówka: Ten filtr wyszukuje tekst w każdym elemencie listy.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}