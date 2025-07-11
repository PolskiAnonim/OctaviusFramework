package org.octavius.report.column.type.special

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.data.FilterData

/**
 * Specjalna kolumna, konfigurująca przestrzeń o konkretnym przeznaczeniu.
 * Nie jest sortowalna ani filtrowalna.
 */
abstract class SpecialColumn(technicalName: String, width: Float) : ReportColumn(
    technicalName,
    "",
    width,
    filterable = false,
    sortable = false
) {
    // Nie tworzy danych filtra
    override fun createFilterData(): FilterData = throw NotImplementedError("SpecialColumn does not support filtering.")

    @Composable
    override fun FilterRenderer(data: FilterData) {
        throw NotImplementedError("SpecialColumn does not support filtering.")
    }

    @Composable
    abstract override fun RenderCell(item: Any?, modifier: Modifier)
}