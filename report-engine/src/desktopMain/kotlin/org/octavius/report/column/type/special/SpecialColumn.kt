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
 * Klasa bazowa dla specjalnych kolumn w systemie raportowania.
 * Specjalne kolumny to kolumny funkcjonalne (np. akcje, kontrolki) które nie wyświetlają danych z bazy.
 * 
 * Charakterystyki:
 * - Pusty nagłówek
 * - Stała szerokość w dp
 * - Nie obsługują filtrowania ani sortowania
 * - Wymagają pełnego kontekstu (ReportState, onEvent) do renderowania
 * 
 * @param width Stała szerokość kolumny w dp
 */
abstract class SpecialColumn(width: Dp) : ReportColumn(
    "",
    width = ColumnWidth.Fixed(width),
    filterable = false,
    sortable = false
) {
    /**
     * Specjalne kolumny nie obsługują filtrowania.
     * Ta metoda rzuca wyjątek jeśli zostanie wywołana.
     */
    final override fun createFilter(): Filter<*> = throw NotImplementedError("SpecialColumn does not support filtering.")

    /**
     * Ta metoda jest nadpisywana jako final aby zapewnić, że nikt jej nie wywoła dla SpecialColumn.
     * Zawsze powinna być używana wersja z pełnym kontekstem (ReportState i onEvent).
     * 
     * @throws NotImplementedError Zawsze rzuca wyjątek
     */
    @Composable
    final override fun RenderCell(item: Any?, modifier: Modifier) {
        throw NotImplementedError("For SpecialColumn, use the RenderCell overload with ReportState and onEvent.")
    }

    /**
     * Renderuje zawartość komórki specjalnej z pełnym kontekstem raportu.
     * Specjalne kolumny wymagają dostępu do stanu raportu i funkcji obsługi zdarzeń.
     * 
     * @param item Dane wiersza (zwykle Map<String, Any?>)
     * @param reportState Stan raportu zawierający filtry, sortowanie itp.
     * @param onEvent Funkcja obsługi zdarzeń raportowania
     * @param modifier Modyfikator Compose dla komórki
     */
    @Composable
    abstract fun RenderCell(item: Any?, reportState: ReportState, onEvent: (ReportEvent) -> Unit, modifier: Modifier)
}