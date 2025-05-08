package org.octavius.novels.report.filter.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.TextFilterType
import org.octavius.novels.report.filter.Filter

class StringFilter(columnName: String): Filter(columnName) {

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val textFilter = filter as FilterValue.TextFilter

        // Gdy nie mamy wartości do filtrowania
        if (textFilter.value.value.isEmpty() && textFilter.nullHandling.value == NullHandling.Ignore) {
            return ""
        }

        // Escape wartości tekstu dla SQL
        val escapedValue = textFilter.value.value.replace("'", "''")

        // Określenie czy wyszukiwanie powinno być case-sensitive
        val columnExpr = if (textFilter.caseSensitive.value) columnName else "LOWER($columnName)"
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
            NullHandling.Include -> "($baseClause OR $columnName IS NULL)"
            NullHandling.Exclude -> baseClause  // operatory LIKE/= już wykluczają NULL
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
        val caseSensitive = textFilter.caseSensitive
        val nullHandling = textFilter.nullHandling

        Column(modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = filterText.value,
                onValueChange = {
                    filterText.value = it
                    textFilter.markDirty()
                },
                label = { Text("Filtruj") },
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
                                expanded = false
                                filterType.value = type
                                textFilter.markDirty()
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
                        textFilter.markDirty()
                    }
                )
                Text(
                    text = "Uwzględnij wielkość liter",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            NullHandlingPanel(textFilter)
        }
    }
}