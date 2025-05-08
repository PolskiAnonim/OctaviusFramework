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
import org.octavius.novels.report.filter.type.StringFilter
import org.octavius.novels.report.filter.type.StringListFilter

class StringListColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val maxVisibleItems: Int = 3,
    private val separator: String? = null
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun createFilterValue(): FilterValue<*> {
        filter = StringListFilter(name)
        return FilterValue.TextFilter()
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item as? List<String>

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
}