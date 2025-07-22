package org.octavius.report.component

import org.octavius.domain.SortDirection
import org.octavius.report.ReportPaginationState
import org.octavius.report.filter.data.FilterData

data class ReportState(
    val data: List<Map<String, Any?>> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    //-----------------------------------------------------Stronicowanie------------------------------------------------
    val pagination: ReportPaginationState = ReportPaginationState(),
    //------------------------------------------------Widoczne kolumny--------------------------------------------------
    // Klucze wszystkich kolumn w kolejności - tak żeby nie przejmować się czy są widoczne czy nie
    var columnKeysOrder: List<String> = emptyList(),
    // Aktualna kolejność widocznych kolumn
    val visibleColumns: Set<String> = emptySet(),
    // Oryginalna kolejność widocznych kolumn
    val originalVisibleColumns: Set<String> = emptySet(),
    //----------------------------------------------Zmiany domyślnego SQLa----------------------------------------------
    // Kolejność sortowania
    val sortOrder: List<Pair<String, SortDirection>> = emptyList(),
    // Wartości filtrów (tylko FilterData, nie Filter)
    val filterData: Map<String, FilterData> = emptyMap(),
    // Wyszukiwanie ogólne - w pasku na górze
    val searchQuery: String = ""
) {


    companion object {
        fun initial(structure: ReportStructure): ReportState {
            val allColumns = structure.getAllColumns()
            return ReportState(
                isLoading = true,
                columnKeysOrder = allColumns.keys.toList(),
                visibleColumns = allColumns.keys.toSet(),
                originalVisibleColumns = allColumns.keys.toSet(),
                filterData = allColumns.filterValues { it.filterable }.mapValues { it.value.createFilterAndFilterData()!! }
            )
        }
    }
}