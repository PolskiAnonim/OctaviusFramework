package org.octavius.report.column.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.CellRendererUtils
import org.octavius.report.FilterMode
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.data.FilterData
import kotlin.math.min

class MultiRowColumn(
    private val wrappedColumn: ReportColumn,
    private val maxVisibleItems: Int = 3
) : ReportColumn(
    wrappedColumn.databaseColumnName,
    wrappedColumn.header,
    wrappedColumn.width,
    wrappedColumn.filterable,
    wrappedColumn.sortable
) {

    override fun createFilterData(): FilterData {
        // W tym przypadku oryginalna kolumna będzie posiadała filterable = true czyli nie zwróci null
        val filterData = wrappedColumn.getFilterData()!!
        
        // Dla MultiRowColumn zawsze ustawiamy tryb ListAny (zmiana na kontekst listy)
        filterData.mode.value = FilterMode.ListAny
        
        return filterData
    }

    @Composable
    override fun FilterRenderer(data: FilterData) {
        wrappedColumn.FilterRenderer(data)
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item as? List<Any?>

        if (value.isNullOrEmpty()) {
            CellRendererUtils.StandardCellWrapper(
                modifier = modifier,
                alignment = Alignment.CenterStart
            ) {
                Text(
                    text = "",
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
                value.subList(0, min(maxVisibleItems, value.size)).forEachIndexed { index, item ->
                    wrappedColumn.RenderCell(item, modifier)
                    
                    // Dodaj separator między elementami (ale nie po ostatnim)
                    if (index < min(maxVisibleItems, value.size) - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}