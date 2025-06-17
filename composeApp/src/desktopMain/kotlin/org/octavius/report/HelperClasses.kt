package org.octavius.report

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class ReportState(
    // Obecna strona
    val currentPage: MutableState<Int> = mutableStateOf(0),
    // Razem stron
    val totalPages: MutableState<Int> = mutableStateOf(1),
    // Rozmiar strony
    val pageSize: MutableState<Int> = mutableStateOf(10),
    // Wyszukiwanie ogólne - w pasku na górze
    val searchQuery: MutableState<String> = mutableStateOf(""),
    // Kolejność sortowania
    val sortOrder: MutableState<List<Pair<String, SortDirection>>> = mutableStateOf(listOf()),
    // Wartości filtrów (tylko FilterData, nie Filter)
    val filterValues: MutableState<Map<String, FilterData<*>>> = mutableStateOf(mapOf()),
    // Widoczne kolumny
    val visibleColumns: MutableState<Set<String>> = mutableStateOf(setOf()),
) {
    // Initialize state - docelowo z tabelek w bazie jak są ustawienia + jakieś domyślne widoki - sortowania itd

//    companion object {
//        fun fromConfiguration(config: ReportConfiguration): ReportState {
//            return ReportState(
//                pageSize = mutableStateOf(config.defaultPageSize),
//                sortOrder = mutableStateOf(config.defaultSortBy),
//                visibleColumns = mutableStateOf(config.defaultVisibleColumns)
//            )
//        }
//    }
}


enum class SortDirection {
    ASC, // Rosnąca
    DESC, // Malejąca
    UNSPECIFIED
}

data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)