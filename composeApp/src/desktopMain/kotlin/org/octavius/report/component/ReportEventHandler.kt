package org.octavius.report.component;

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.launch
import org.octavius.report.ReportEvent
import org.octavius.report.management.ReportConfiguration

// Jako że ReportEventHandler zarządza pobieraniem danych to on będzie głównie używał
// DataManagera - żeby jednak ReportHandler był ciągle centralnym elementem on go inicjalizuje
class ReportEventHandler(
  private val coroutineScope: CoroutineScope,
  private val dataManager: ReportDataManager,
  private val reportStructure: ReportStructure,
  private val stateFlow: MutableStateFlow<ReportState>
) {

  private var dataFetchJob: Job? = null


  fun onEvent(event: ReportEvent) {
    val currentState = stateFlow.value
    val newState = reduceState(currentState, event)
    
    // Aktualizuj stan
    if (newState != currentState) {
      stateFlow.value = newState
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
          val defaultFilterData = reportStructure.getColumn(event.columnKey)!!.createFilterAndFilterData()!!
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

      stateFlow.value = stateFlow.value.copy(isLoading = true, error = null)
      
      try {
        val (newData, newPagination) = dataManager.fetchData(state)
        stateFlow.value = stateFlow.value.copy(isLoading = false, data = newData, pagination = newPagination)
      } catch (e: Exception) {
        e.printStackTrace()
        stateFlow.value = stateFlow.value.copy(isLoading = false, error = "Błąd pobierania danych: ${e.message}")
      }
    }
  }

  private fun applyConfiguration(state: ReportState, configuration: ReportConfiguration): ReportState {
    val configData = configuration.configuration

    // Deserializuj filtry, tworząc nową mapę stanów filtrów
    val newFilterDataMap = state.filterData.toMutableMap()
    configData.filters.forEach { filterConfig ->
      val columnKey = filterConfig.columnName
      val column = reportStructure.getColumn(columnKey)

      if (column?.filter != null) {
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

}
