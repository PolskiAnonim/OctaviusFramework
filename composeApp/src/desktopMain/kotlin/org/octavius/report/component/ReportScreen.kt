package org.octavius.report.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.navigator.Screen

abstract class ReportScreen : Screen {

    abstract val reportHandler: ReportHandler
    abstract val reportName: String

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
                filter.dirtyState.value
            }
        }

        // Efekt pobierający dane po zmianie parametrów
        LaunchedEffect(
            reportState.currentPage.value,
            reportState.searchQuery.value,
            reportState.pageSize.value,
            reportState.sortOrder.value,
            filteringState.value
        ) {
            reportHandler.fetchData(
                page = reportState.currentPage.value,
                searchQuery = reportState.searchQuery.value,
                pageSize = reportState.pageSize.value
            ) { result, pages ->
                dataList = result
                reportState.totalPages.value = pages.toInt()
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
                            reportState.currentPage.value = 0
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
                currentPage = reportState.currentPage.value,
                totalPages = reportState.totalPages.value,
                pageSize = reportState.pageSize.value,
                onPageChange = { newPage ->
                    reportState.currentPage.value = newPage
                    coroutineScope.launch {
                        lazyListState.scrollToItem(0)
                    }
                },
                onPageSizeChange = { newPageSize ->
                    reportState.pageSize.value = newPageSize
                    reportState.currentPage.value = 0
                }
            )
        }
        
        // Dialog konfiguracji
        if (configurationDialogVisible) {
            ReportConfigurationDialog(
                reportName = reportName,
                reportState = reportState,
                filters = reportHandler.getFilters(),
                onDismiss = { configurationDialogVisible = false },
                onConfigurationApplied = {
                    // Po zastosowaniu konfiguracji, odśwież dane
                    coroutineScope.launch {
                        reportHandler.fetchData(
                            page = reportState.currentPage.value,
                            searchQuery = reportState.searchQuery.value,
                            pageSize = reportState.pageSize.value
                        ) { result, pages ->
                            dataList = result
                            reportState.totalPages.value = pages.toInt()
                        }
                    }
                }
            )
        }
    }
}