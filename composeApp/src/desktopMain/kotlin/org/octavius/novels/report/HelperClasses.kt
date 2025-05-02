package org.octavius.novels.report

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

data class ReportState(
        val currentPage: MutableState<Int> = mutableStateOf(1),
        val totalPages: MutableState<Int> = mutableStateOf(1),
        val pageSize: MutableState<Int> = mutableStateOf(10),
        val searchQuery: MutableState<String> = mutableStateOf(""),
        val sorting: SnapshotStateMap<String,SortValue> = mutableStateMapOf(),
        val filtering: SnapshotStateMap<String, FilterValue<*>> = mutableStateMapOf(),
        val columnVisibility: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
)

// Sorting
data class SortValue(
        val columnName: MutableState<String>,
        val direction: MutableState<SortDirection> = mutableStateOf(SortDirection.ASC),
)

enum class SortDirection {
        ASC, // Rosnąca
        DESC // Malejąca
}

data class Query(
        val sql: String ,
        val params: Array<Any?>  = emptyArray()
)