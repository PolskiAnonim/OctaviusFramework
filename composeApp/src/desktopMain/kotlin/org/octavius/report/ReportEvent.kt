package org.octavius.report

import org.octavius.domain.SortDirection
import org.octavius.report.filter.data.FilterData
import org.octavius.report.management.ReportConfiguration

/**
 * Definiuje wszystkie możliwe akcje/intencje użytkownika,
 * które mogą zmienić stan raportu.
 */
sealed interface ReportEvent {
    
    fun triggersDataReload(): Boolean = when (this) {
        is Initialize, is RefreshData, is SearchQueryChanged, 
        is PageChanged, is PageSizeChanged, is SortOrderChanged,
        is FilterChanged, is ClearFilter, is ApplyConfiguration -> true
        
        is ColumnVisibilityChanged, is ColumnOrderChanged, 
        is SaveConfiguration, is DeleteConfiguration -> false
    }
    // Inicjalizacja i odświeżanie
    object Initialize : ReportEvent
    object RefreshData : ReportEvent

    // Wyszukiwanie
    data class SearchQueryChanged(val query: String) : ReportEvent

    // Paginacja
    data class PageChanged(val newPage: Long) : ReportEvent
    data class PageSizeChanged(val newSize: Int) : ReportEvent

    // Sortowanie
    data class SortOrderChanged(val newSortOrder: List<Pair<String, SortDirection>>) : ReportEvent

    // Filtry
    data class FilterChanged(val columnKey: String, val newFilterData: FilterData) : ReportEvent
    data class ClearFilter(val columnKey: String) : ReportEvent

    // Zarządzanie kolumnami
    data class ColumnVisibilityChanged(val newVisibleColumns: Set<String>) : ReportEvent
    data class ColumnOrderChanged(val newColumnOrder: List<String>) : ReportEvent

    // Konfiguracje
    data class ApplyConfiguration(val configuration: ReportConfiguration) : ReportEvent
    data class SaveConfiguration(val name: String, val description: String?, val isDefault: Boolean) : ReportEvent
    data class DeleteConfiguration(val name: String) : ReportEvent
}