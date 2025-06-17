package org.octavius.report.filter.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterData
import org.octavius.report.NullHandling
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.Filter

class StringFilter(columnName: String) : Filter(columnName) {

    override fun constructWhereClause(filter: FilterData<*>): String {
        val filterData = filter as FilterData.StringData

        // Gdy nie mamy wartości do filtrowania
        if (filterData.value.value.isEmpty() && filterData.nullHandling.value == NullHandling.Ignore) {
            return ""
        }

        // Escape wartości tekstu dla SQL
        val escapedValue = filterData.value.value.replace("'", "''")

        // Określenie czy wyszukiwanie powinno być case-sensitive
        val columnExpr = if (filterData.caseSensitive.value) columnName else "LOWER($columnName)"
        val valueExpr = if (filterData.caseSensitive.value) "'$escapedValue'" else "LOWER('$escapedValue')"

        // Budowanie podstawowej klauzuli w zależności od typu filtra
        val baseClause = when (filterData.filterType.value) {
            StringFilterDataType.Exact ->
                "$columnExpr = $valueExpr"

            StringFilterDataType.StartsWith ->
                "$columnExpr LIKE '${escapedValue}%'"

            StringFilterDataType.EndsWith ->
                "$columnExpr LIKE '%${escapedValue}'"

            StringFilterDataType.Contains ->
                "$columnExpr LIKE '%${escapedValue}%'"

            StringFilterDataType.NotContains ->
                "$columnExpr NOT LIKE '%${escapedValue}%'"
        }

        // Łączenie podstawowej klauzuli z obsługą nulli
        return when (filterData.nullHandling.value) {
            NullHandling.Ignore -> baseClause
            NullHandling.Include -> "($baseClause OR $columnName IS NULL)"
            NullHandling.Exclude -> baseClause  // operatory LIKE/= już wykluczają NULL
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
        val caseSensitive = filterData.caseSensitive

        Column(modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = filterText.value,
                onValueChange = {
                    filterText.value = it
                    filterData.markDirty()
                },
                label = { Text("Filtruj") },
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
                        StringFilterDataType.Exact -> "Dokładnie"
                        StringFilterDataType.StartsWith -> "Zaczyna się od"
                        StringFilterDataType.EndsWith -> "Kończy się na"
                        StringFilterDataType.Contains -> "Zawiera"
                        StringFilterDataType.NotContains -> "Nie zawiera"
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
                        StringFilterDataType.Contains to "Zawiera",
                        StringFilterDataType.StartsWith to "Zaczyna się od",
                        StringFilterDataType.EndsWith to "Kończy się na",
                        StringFilterDataType.Exact to "Dokładnie",
                        StringFilterDataType.NotContains to "Nie zawiera"
                    ).forEach { (type, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                filterType.value = type
                                filterData.markDirty()
                            },
                            trailingIcon = if (filterType.value == type) {
                                { Icon(Icons.Default.Check, null) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Opcja dla wielkości liter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = caseSensitive.value,
                    onCheckedChange = {
                        caseSensitive.value = it
                        filterData.markDirty()
                    }
                )
                Text(
                    text = "Uwzględnij wielkość liter",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NullHandlingPanel(filterData)
        }
    }
}