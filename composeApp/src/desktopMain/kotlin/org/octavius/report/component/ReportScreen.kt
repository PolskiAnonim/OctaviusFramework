package org.octavius.report.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.navigator.Screen
import org.octavius.report.ui.PaginationComponent

abstract class ReportScreen : Screen {
    abstract override val title: String
    abstract val reportHandler: ReportHandler

    @Composable
    protected open fun AddMenu() {
        Box {}
    }

    @Composable
    override fun Content() {
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var dataList by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
        var addMenuExpanded by remember { mutableStateOf(false) }
        var configurationDialogVisible by remember { mutableStateOf(false) }

        val reportState = reportHandler.getReportState()

        // Dla śledzenia zmian filtrów
        val filteringState = derivedStateOf {
            reportState.filterValues.value.mapValues { (_, filter) ->
                filter.isActive()
            }
        }

        // Efekt pobierający dane po zmianie parametrów
        LaunchedEffect(
            reportState.pagination.currentPage.value,
            reportState.searchQuery.value,
            reportState.pagination.pageSize.value,
            reportState.sortOrder.value,
            filteringState.value
        ) {
            reportHandler.fetchData(
                page = reportState.pagination.currentPage.value.toInt(),
                searchQuery = reportState.searchQuery.value,
                pageSize = reportState.pagination.pageSize.value
            ) { result, pages ->
                dataList = result
                reportState.pagination.totalPages.value = pages
            }
        }

        Column(modifier = Modifier) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                item {
                    // Panel zarządzania kolumnami
                    ColumnManagementPanel(
                        columnNames = reportHandler.getColumns().map { it.key to it.value.header }.toMap(),
                        reportState = reportState,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                item {
                    // Pasek wyszukiwania
                    ReportSearchBar(
                        searchQuery = reportState.searchQuery.value,
                        onSearchChange = { query ->
                            reportState.searchQuery.value = query
                            reportState.pagination.resetPage()
                        },
                        onAddMenuClick = { addMenuExpanded = !addMenuExpanded },
                        addMenuExpanded = addMenuExpanded,
                        onAddMenuDismiss = { addMenuExpanded = false },
                        addMenuContent = { AddMenu() },
                        onConfigurationClick = { configurationDialogVisible = true }
                    )
                }

                // Tabela z danymi
                reportTable(reportHandler, reportState, dataList)
            }

            // Paginacja
            PaginationComponent(
                reportState.pagination
            )
        }
        
        // Dialog konfiguracji
        if (configurationDialogVisible) {
            ReportConfigurationDialog(
                reportName = reportHandler.getReportName(),
                reportState = reportState,
                onDismiss = { configurationDialogVisible = false },
                onConfigurationApplied = {
                    // Po zastosowaniu konfiguracji, odśwież dane
                    coroutineScope.launch {
                        reportHandler.fetchData(
                            page = reportState.pagination.currentPage.value.toInt(),
                            searchQuery = reportState.searchQuery.value,
                            pageSize = reportState.pagination.pageSize.value
                        ) { result, pages ->
                            dataList = result
                            reportState.pagination.totalPages.value = pages
                        }
                    }
                }
            )
        }
    }
}