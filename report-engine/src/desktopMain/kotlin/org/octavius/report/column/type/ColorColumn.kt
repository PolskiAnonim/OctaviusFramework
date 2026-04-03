package org.octavius.report.column.type

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.octavius.report.CellRendererUtils
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter
import org.octavius.report.filter.type.StringFilter
import org.octavius.util.ColorUtils

/**
 * Kolumna do wyświetlania kolorów w raporcie.
 * Oczekuje ciągu znaków w formacie hex (#RRGGBB lub #AARRGGBB).
 * 
 * @param header Nagłówek kolumny
 * @param showHex Czy wyświetlać kod hex koloru (domyślnie true)
 * @param width Względna szerokość kolumny (domyślnie 1.0)
 * @param sortable Czy kolumna obsługuje sortowanie (domyślnie true)
 * @param filterable Czy kolumna obsługuje filtrowanie (domyślnie true)
 */
class ColorColumn(
    header: String,
    val showHex: Boolean = true,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true
) : ReportColumn(header, ColumnWidth.Flexible(width), filterable, sortable) {

    override fun createFilter(): Filter<*> {
        return StringFilter()
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val hex = item as? String
        val color = hex?.let { ColorUtils.hexToColor(it) } ?: Color.Transparent

        CellRendererUtils.StandardCellWrapper(
            modifier = modifier,
            alignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Podgląd koloru
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                if (showHex && hex != null) {
                    Text(
                        text = hex,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
