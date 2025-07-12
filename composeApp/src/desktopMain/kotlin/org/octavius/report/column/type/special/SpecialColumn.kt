package org.octavius.report.column.type.special

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter

/**
 * Specjalna kolumna, konfigurująca przestrzeń o konkretnym przeznaczeniu.
 * Nie jest sortowalna ani filtrowalna.
 * Posiada stałą ustaloną szerokość
 */
abstract class SpecialColumn(technicalName: String, width: Dp) : ReportColumn(
    technicalName,
    "",
    width = ColumnWidth.Fixed(width),
    filterable = false,
    sortable = false
) {
    // Nie tworzy danych filtra
    override fun createFilter(): Filter<*> = throw NotImplementedError("SpecialColumn does not support filtering.")

    @Composable
    abstract override fun RenderCell(item: Any?, modifier: Modifier)
}