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
import org.octavius.report.filter.type.IntervalFilter
import kotlin.time.Duration // Import Duration
import kotlin.time.ExperimentalTime // Wymagane dla Duration

/**
 * Kolumna do wyświetlania wartości typu Duration (interwałów czasowych) w raporcie.
 * Obsługuje filtrowanie przez interwały.
 *
 * @param header Nagłówek kolumny
 * @param width Względna szerokość kolumny (domyślnie 1.0)
 * @param sortable Czy kolumna obsługuje sortowanie (domyślnie true)
 * @param filterable Czy kolumna obsługuje filtrowanie (domyślnie true)
 * @param formatter Funkcja formatująca wartość Duration do wyświetlenia (domyślnie po prostu toString())
 */
@OptIn(ExperimentalTime::class)
class IntervalColumn(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    private val formatter: (Duration?) -> String = { it?.toString() ?: "" } // Domyślny formatter
) : ReportColumn(header, ColumnWidth.Flexible(width), filterable, sortable) {

    override fun createFilter(): Filter<*> {
        return IntervalFilter()
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val value = item as? Duration

        CellRendererUtils.StandardCellWrapper(
            modifier = modifier,
            alignment = Alignment.CenterEnd // Wyrównanie do prawej, jak dla liczb
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        }
    }
}