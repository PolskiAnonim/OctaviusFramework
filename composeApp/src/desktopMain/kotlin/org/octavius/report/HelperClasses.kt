package org.octavius.report

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class ReportState(
    val currentPage: MutableState<Int> = mutableStateOf(0),
    val totalPages: MutableState<Int> = mutableStateOf(1),
    val pageSize: MutableState<Int> = mutableStateOf(10),
    val searchQuery: MutableState<String> = mutableStateOf(""),
)

data class ColumnState(
    val sortDirection: MutableState<SortDirection> = mutableStateOf(SortDirection.ASC),
    val sortOrder: MutableState<Int?> = mutableStateOf(null),
    val filtering: MutableState<FilterData<*>?> = mutableStateOf(null),
    val visible: MutableState<Boolean> = mutableStateOf(true)
)

enum class SortDirection {
    ASC, // Rosnąca
    DESC, // Malejąca
    UNSPECIFIED
}

data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)