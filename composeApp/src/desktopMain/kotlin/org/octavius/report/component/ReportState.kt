package org.octavius.report.component

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import org.octavius.domain.SortDirection
import org.octavius.report.ReportPagination
import org.octavius.report.filter.data.FilterData

class ReportState {
    private val _data = mutableStateOf<List<Map<String,Any?>>>(emptyList())
    private val _loading = mutableStateOf(false)
    private val _error = mutableStateOf<String?>(null)

    var data: List<Map<String, Any?>>
        get() = _data.value
        set(value) {
            _data.value = value
        }
    val loading: Boolean get() = _loading.value
    val error: String? get() = _error.value

    fun setLoading(loading: Boolean) {
        _loading.value = loading
    }

    fun setError(error: String?) {
        _error.value = error
    }

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
    val filterData: MutableState<Map<String, FilterData>> = mutableStateOf(mapOf())
    // Wyszukiwanie ogólne - w pasku na górze
    val searchQuery: MutableState<String> = mutableStateOf("")

    fun initialize(columnKeys: Set<String>, reportConfiguration: String) {
        // Merge oryginalnego configa i configa z bazy jak będzie
        originalVisibleColumns = columnKeys
        visibleColumns.value = originalVisibleColumns
        this.columnKeys = columnKeys.toMutableStateList()
    }
}