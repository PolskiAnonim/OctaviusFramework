package org.octavius.report

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf


data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)

data class ReportPagination(
    val currentPage: MutableState<Long> = mutableStateOf(0),
    val totalPages: MutableState<Long> = mutableStateOf(1),
    val totalItems: MutableState<Long> = mutableStateOf(0),
    val pageSize: MutableState<Int> = mutableStateOf(10)
) {
    fun resetPage() {
        currentPage.value = 0
    }
}