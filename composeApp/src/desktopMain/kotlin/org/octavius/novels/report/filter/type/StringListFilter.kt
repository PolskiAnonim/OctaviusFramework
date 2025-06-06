package org.octavius.novels.report.filter.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.TextFilterType
import org.octavius.novels.report.filter.Filter

class StringListFilter(columnName: String) : Filter(columnName) {

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
                "$columnName @> ARRAY[$valueExpr]"  // Sprawdza czy lista zawiera dokładnie tę wartość

            TextFilterType.StartsWith ->
                "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '${escapedValue}%')"

            TextFilterType.EndsWith ->
                "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}')"

            TextFilterType.Contains ->
                "EXISTS (SELECT FROM unnest($columnName) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}%')"

            TextFilterType.NotContains ->
                "NOT EXISTS (SELECT FROM unnest($columnName) AS elem WHERE " +
                        (if (textFilter.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}%')"
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (textFilter.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $columnName IS NULL)"
            NullHandling.Exclude -> "($baseClause AND $columnName IS NOT NULL)"
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>
    ) {

        val textFilter = currentFilter as? FilterValue.TextFilter ?: return
        val filterText = textFilter.value
        val filterType = textFilter.filterType

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
                    textFilter.markDirty()
                },
                label = { Text("Szukaj w liście") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (filterText.value.isNotEmpty()) {
                        IconButton(onClick = {
                            filterText.value = ""
                            textFilter.markDirty()
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
                    value = when (filterType.value) {
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
                                expanded = false
                                filterType.value = type
                                textFilter.markDirty()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NullHandlingPanel(textFilter)

            Text(
                text = "Wskazówka: Ten filtr wyszukuje tekst w każdym elemencie listy.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}