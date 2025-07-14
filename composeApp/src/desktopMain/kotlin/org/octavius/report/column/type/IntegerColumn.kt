package org.octavius.report.column.type

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.octavius.report.CellRendererUtils
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter
import org.octavius.report.filter.type.NumberFilter

/**
 * Kolumna do wyświetlania liczb całkowitych w raporcie.
 * Obsługuje filtrowanie numeryczne.
 * 
 * @param header Nagłówek kolumny
 * @param width Względna szerokość kolumny (domyślnie 1.0)
 * @param sortable Czy kolumna obsługuje sortowanie (domyślnie true)
 * @param filterable Czy kolumna obsługuje filtrowanie (domyślnie true)
 * @param formatter Funkcja formatowania liczb do wyświetlenia (domyślnie toString)
 */
class IntegerColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    private val formatter: (Int?) -> String = { it?.toString() ?: "" }
) : ReportColumn(header, ColumnWidth.Flexible(width), filterable, sortable) {

    override fun createFilter(): Filter<*> {
        return NumberFilter(Int::class) { it.toIntOrNull() }
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val value = item as? Int

        CellRendererUtils.StandardCellWrapper(
            modifier = modifier,
            alignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        }
    }
}