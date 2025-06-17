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
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.Filter

class StringListFilter(columnName: String) : Filter(columnName) {

    override fun constructWhereClause(filter: FilterData<*>): String {
        val filterData = filter as FilterData.StringData

        // Gdy nie mamy wartości do filtrowania
        if (filterData.value.value.isEmpty() && filterData.nullHandling.value == NullHandling.Ignore) {
            return ""
        }

        // Escape wartości tekstu dla SQL
        val escapedValue = filterData.value.value.replace("'", "''")

        // Określenie czy wyszukiwanie powinno być case-sensitive
        val valueExpr = if (filterData.caseSensitive.value) "'$escapedValue'" else "LOWER('$escapedValue')"

        // Budowanie klauzuli dla listy tekstów
        // W przypadku listy musimy sprawdzić, czy jakikolwiek element spełnia warunek
        val baseClause = when (filterData.filterType.value) {
            StringFilterDataType.Exact ->
                "$columnName @> ARRAY[$valueExpr]"  // Sprawdza czy lista zawiera dokładnie tę wartość

            StringFilterDataType.StartsWith ->
                "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE " +
                        (if (filterData.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '${escapedValue}%')"

            StringFilterDataType.EndsWith ->
                "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE " +
                        (if (filterData.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}')"

            StringFilterDataType.Contains ->
                "EXISTS (SELECT FROM unnest($columnName) AS elem WHERE " +
                        (if (filterData.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}%')"

            StringFilterDataType.NotContains ->
                "NOT EXISTS (SELECT FROM unnest($columnName) AS elem WHERE " +
                        (if (filterData.caseSensitive.value) "elem" else "LOWER(elem)") +
                        " LIKE '%${escapedValue}%')"
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (filterData.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $columnName IS NULL)"
            NullHandling.Exclude -> "($baseClause AND $columnName IS NOT NULL)"
        }
    }

    override fun createFilterData(): FilterData<*> {
        return FilterData.StringData()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderFilter(
        currentFilter: FilterData<*>
    ) {

        val filterData = currentFilter as? FilterData.StringData ?: return
        val filterText = filterData.value
        val filterType = filterData.filterType

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
                    filterData.markDirty()
                },
                label = { Text("Szukaj w liście") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (filterText.value.isNotEmpty()) {
                        IconButton(onClick = {
                            filterText.value = ""
                            filterData.markDirty()
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
                        StringFilterDataType.Exact -> "Element dokładnie równy"
                        StringFilterDataType.StartsWith -> "Element zaczyna się od"
                        StringFilterDataType.EndsWith -> "Element kończy się na"
                        StringFilterDataType.Contains -> "Element zawiera"
                        StringFilterDataType.NotContains -> "Element nie zawiera"
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
                        StringFilterDataType.Contains to "Element zawiera",
                        StringFilterDataType.StartsWith to "Element zaczyna się od",
                        StringFilterDataType.EndsWith to "Element kończy się na",
                        StringFilterDataType.Exact to "Element dokładnie równy",
                        StringFilterDataType.NotContains to "Element nie zawiera"
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

            NullHandlingPanel(filterData)

            Text(
                text = "Wskazówka: Ten filtr wyszukuje tekst w każdym elemencie listy.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}