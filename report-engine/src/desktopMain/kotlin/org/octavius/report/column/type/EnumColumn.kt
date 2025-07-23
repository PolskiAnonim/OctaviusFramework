package org.octavius.report.column.type

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.octavius.domain.EnumWithFormatter
import org.octavius.report.CellRendererUtils
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter
import org.octavius.report.filter.type.EnumFilter
import kotlin.reflect.KClass

/**
 * Kolumna do wyświetlania wartości enum w raporcie.
 * Wymaga aby enum implementował EnumWithFormatter dla poprawnego wyświetlania.
 * Obsługuje filtrowanie przez dostępnych wartości Enum.
 * 
 * @param T Typ enum dziedziczący po EnumWithFormatter
 * @param header Nagłówek kolumny
 * @param width Względna szerokość kolumny (domyślnie 1.0)
 * @param sortable Czy kolumna obsługuje sortowanie (domyślnie true)
 * @param filterable Czy kolumna obsługuje filtrowanie (domyślnie true)
 * @param enumClass Klasa enum używana do tworzenia filtra
 */
class EnumColumn<T>(
    header: String,
    width: Float = 1f,
    sortable: Boolean = true,
    filterable: Boolean = true,
    val enumClass: KClass<T>,
) : ReportColumn(header, ColumnWidth.Flexible(width), filterable, sortable)
        where T : Enum<T>, T : EnumWithFormatter<T> {

    override fun createFilter(): Filter<*> {
        return EnumFilter(enumClass)
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item as? T

        CellRendererUtils.StandardCellWrapper(
            modifier = modifier,
            alignment = Alignment.CenterStart
        ) {
            Text(
                text = value?.toDisplayString() ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}