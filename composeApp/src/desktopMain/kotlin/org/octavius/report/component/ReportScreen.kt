package org.octavius.report.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        var addMenuExpanded by remember { mutableStateOf(false) }
        var configurationDialogVisible by remember { mutableStateOf(false) }

        val reportState = reportHandler.getReportState()

        reportHandler.DataFetcher()

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
                reportTable(reportHandler, reportState, reportState.data)
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

                }
            )
        }
    }
}