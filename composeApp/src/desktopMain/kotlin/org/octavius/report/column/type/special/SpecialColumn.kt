package org.octavius.report.column.type.special

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.octavius.report.ColumnWidth
import org.octavius.report.ReportEvent
import org.octavius.report.column.ReportColumn
import org.octavius.report.component.ReportState
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

    /**
     * Ta metoda jest nadpisywana, aby zapewnić, że nikt jej nie wywoła dla SpecialColumn.
     * Zawsze powinna być używana wersja z pełnym kontekstem.
     */
    @Composable
    final override fun RenderCell(item: Any?, modifier: Modifier) {
        throw NotImplementedError("For SpecialColumn, use the RenderCell overload with ReportState and onEvent.")
    }

    // Metoda wygenerowania komórki z całym kontekstem
    @Composable
    abstract fun RenderCell(item: Any?, reportState: ReportState, onEvent: (ReportEvent) -> Unit, modifier: Modifier)
}