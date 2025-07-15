package org.octavius.report.component

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.octavius.domain.SortDirection
import org.octavius.report.ReportEvent
import org.octavius.report.configuration.ReportConfiguration
import org.octavius.report.configuration.ReportConfigurationManager

val LocalReportHandler = compositionLocalOf<ReportHandler> { error("No ReportHandler provided") }

class ReportHandler(
    private val coroutineScope: CoroutineScope,
    val reportStructure: ReportStructure
) {

    private var dataFetchJob: Job? = null

    // Inicjalizujemy stan początkowy.
    private val _state: MutableStateFlow<ReportState> = MutableStateFlow(ReportState.initial(reportStructure))
    val state = _state.asStateFlow()

    private val dataManager = ReportDataManager(reportStructure)
    private val configManager = ReportConfigurationManager()

    init {
        // Załadowanie domyślnej konfiguracji
        loadDefaultConfiguration()
    }

    //------------------------------------------Event Handling----------------------------------------------------------

    fun onEvent(event: ReportEvent) {
        val currentState = _state.value
        val newState = reduceState(currentState, event)
        
        // Aktualizuj stan
        if (newState != currentState) {
            _state.value = newState
        }
        
        // Sprawdź czy event wymaga przeładowania danych
        if (event.triggersDataReload()) {
            fetchData(newState)
        }
    }

    private fun reduceState(currentState: ReportState, event: ReportEvent): ReportState {
        return when (event) {
            is ReportEvent.SearchQueryChanged -> currentState.copy(
                searchQuery = event.query,
                pagination = currentState.pagination.resetPage()
            )
            is ReportEvent.FilterChanged -> {
                val newFilters = currentState.filterData.toMutableMap().apply {
                    this[event.columnKey] = event.newFilterData
                }
                currentState.copy(filterData = newFilters, pagination = currentState.pagination.resetPage())
            }
            is ReportEvent.ClearFilter -> {
                val newFilters = currentState.filterData.toMutableMap().apply {
                    val defaultFilterData = reportStructure.getColumn(event.columnKey).createFilterAndFilterData()!!
                    this[event.columnKey] = defaultFilterData
                }
                currentState.copy(filterData = newFilters, pagination = currentState.pagination.resetPage())
            }
            is ReportEvent.PageChanged -> currentState.copy(
                pagination = currentState.pagination.copy(currentPage = event.newPage)
            )
            is ReportEvent.PageSizeChanged -> currentState.copy(
                pagination = currentState.pagination.copy(pageSize = event.newSize).resetPage()
            )
            is ReportEvent.SortOrderChanged -> currentState.copy(
                sortOrder = event.newSortOrder,
                pagination = currentState.pagination.resetPage()
            )
            is ReportEvent.ColumnOrderChanged -> currentState.copy(columnKeysOrder = event.newColumnOrder)
            is ReportEvent.ColumnVisibilityChanged -> currentState.copy(visibleColumns = event.newVisibleColumns)
            is ReportEvent.ApplyConfiguration -> applyConfiguration(currentState, event.configuration)
            // Eventy które nie zmieniają stanu bezpośrednio
            is ReportEvent.Initialize -> currentState
        }
    }

    private fun fetchData(state: ReportState) {
        dataFetchJob?.cancel()
        dataFetchJob = coroutineScope.launch {

            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val (newData, newPagination) = dataManager.fetchData(state)
                _state.value = _state.value.copy(isLoading = false, data = newData, pagination = newPagination)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(isLoading = false, error = "Błąd pobierania danych: ${e.message}")
            }
        }
    }

    //-------------------------------------------Configuration----------------------------------------------------------

    private fun applyConfiguration(state: ReportState, configuration: ReportConfiguration): ReportState {
        val configData = configuration.configuration

        // Deserializuj filtry, tworząc nową mapę stanów filtrów
        val newFilterDataMap = state.filterData.toMutableMap()
        configData.filters.forEach { filterConfig ->
            val columnKey = filterConfig.columnName
            val column = reportStructure.getColumn(columnKey)

            if (column.filter != null) {
                // Używamy fabryki z klasy Filter do stworzenia nowej instancji FilterData
                val newFilterState = column.filter!!.deserializeData(filterConfig.config)
                newFilterDataMap[columnKey] = newFilterState
            }
        }

        return state.copy(
            pagination = state.pagination.copy(currentPage = 0, pageSize = configData.pageSize),
            visibleColumns = configData.visibleColumns.toSet(),
            columnKeysOrder = configData.columnOrder,
            sortOrder = configData.sortOrder.map { (columnName, sortDirection) -> columnName to sortDirection },
            filterData = newFilterDataMap
        )
    }

    private fun loadDefaultConfiguration() {
        val defaultConfig = configManager.loadDefaultConfiguration(reportStructure.reportName)
        if (defaultConfig != null) {
            onEvent(ReportEvent.ApplyConfiguration(defaultConfig))
        } else {
            onEvent(ReportEvent.Initialize)
        }
    }

    //-------------------------------------Sorting and visibility handling----------------------------------------------

    fun toggleColumnVisibility(
        columnKey: String,
        isVisible: Boolean
    ) {
        if (isVisible) {
            val visibleColumns = state.value.visibleColumns.toMutableSet()
            visibleColumns.remove(columnKey)
            onEvent(ReportEvent.ColumnVisibilityChanged(visibleColumns.toSet()))
        } else {
            val visibleColumns = state.value.visibleColumns.toMutableSet()
            visibleColumns.add(columnKey)
            onEvent(ReportEvent.ColumnVisibilityChanged(visibleColumns.toSet()))
        }
    }

    fun reorderColumns(fromIndex: Int, toIndex: Int) {
        val columnKeysOrder = state.value.columnKeysOrder.toMutableList()
        val item = columnKeysOrder.removeAt(fromIndex)
        columnKeysOrder.add(toIndex, item)
        onEvent(ReportEvent.ColumnOrderChanged(columnKeysOrder))
    }

    fun updateSortDirection(
        columnKey: String,
        newDirection: SortDirection
    ) {
        val currentSort = state.value.sortOrder.toMutableList()
        val index = currentSort.indexOfFirst { it.first == columnKey }
        if (index >= 0) {
            currentSort[index] = columnKey to newDirection
            onEvent(ReportEvent.SortOrderChanged(currentSort))
        }
    }

    fun removeSortColumn(
        columnKey: String
    ) {
        val newSort = state.value.sortOrder.filter { it.first != columnKey }
        onEvent(ReportEvent.SortOrderChanged(newSort))
    }

    fun addSortColumn(
        columnKey: String
    ): Boolean {
        val currentSort = state.value.sortOrder
        if (!currentSort.any { it.first == columnKey }) {
            val newSort = state.value.sortOrder.toMutableList()
            newSort.add(columnKey to SortDirection.Ascending)
            onEvent(ReportEvent.SortOrderChanged(newSort))
            return true
        }
        return false
    }

    fun reorderSortColumns(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val newSort = state.value.sortOrder.toMutableList()
        val item = newSort.removeAt(fromIndex)
        newSort.add(toIndex, item)
        onEvent(ReportEvent.SortOrderChanged(newSort))
    }

}
