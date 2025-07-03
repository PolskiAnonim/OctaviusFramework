package org.octavius.report.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import org.octavius.report.FilterData
import org.octavius.domain.SortDirection
import org.octavius.report.ReportPagination

class ReportState {
    //-----------------------------------------------------Stronicowanie------------------------------------------------
    // Rozmiar strony
    val pagination = ReportPagination()
    //-----------------------------------------------------Kolumny------------------------------------------------------
    // Klucze wszystkich kolumn w kolejności - tak żeby nie przejmować się czy są widoczne czy nie
    lateinit var columnKeys: SnapshotStateList<String>
    //------------------------------------------------Widoczne kolumny--------------------------------------------------
    // Aktualna kolejność widocznych kolumn
    val visibleColumns: MutableState<Set<String>> = mutableStateOf(setOf())
    // Oryginalna kolejność widocznych kolumn
    lateinit var originalVisibleColumns: Set<String>
    //----------------------------------------------Zmiany domyślnego SQLa----------------------------------------------
    // Kolejność sortowania
    val sortOrder: MutableState<List<Pair<String, SortDirection>>> = mutableStateOf(listOf())
    // Wartości filtrów (tylko FilterData, nie Filter)
    val filterValues: MutableState<Map<String, FilterData<*>>> = mutableStateOf(mapOf())
    // Wyszukiwanie ogólne - w pasku na górze
    val searchQuery: MutableState<String> = mutableStateOf("")

    fun initialize(columnKeys: Set<String>, reportConfiguration: String) {
        // Merge oryginalnego configa i configa z bazy jak będzie
        originalVisibleColumns = columnKeys
        visibleColumns.value = originalVisibleColumns
        this.columnKeys = columnKeys.toMutableStateList()
    }
}