package org.octavius.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterData
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.type.StringListFilter

class StringListColumn(
    fieldName: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val maxVisibleItems: Int = 3,
    private val separator: String? = null
) : ReportColumn(fieldName, header, width, filterable, sortable) {

    override fun createFilterValue(): FilterData<*> {
        filter = StringListFilter(name)
        return FilterData.StringData()
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