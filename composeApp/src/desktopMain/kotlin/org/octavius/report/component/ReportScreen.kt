package org.octavius.report.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.octavius.navigator.Screen
import org.octavius.report.management.ColumnManagementPanel
import org.octavius.report.management.ReportConfigurationDialog
import org.octavius.report.ui.PaginationComponent
import org.octavius.report.ui.ReportSearchBar
import org.octavius.report.ui.reportTable

abstract class ReportScreen : Screen {
    abstract override val title: String
    abstract val reportHandler: ReportHandler


    @Composable
    override fun Content() {
        val uiState = rememberReportUIState()
        val reportState = reportHandler.reportState

        reportHandler.DataFetcher()

        Scaffold(
            topBar = {
                ColumnManagementPanel(
                    manageableColumnKeys = reportHandler.reportStructure.manageableColumnKeys,
                    columnNames = reportHandler.reportStructure.getAllColumns().map { it.key to it.value.header }
                        .toMap(),
                    reportState = reportState,
                    modifier = Modifier.padding(8.dp)
                )
            },
            bottomBar = {
                PaginationComponent(reportState.pagination)
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // Pasek wyszukiwania
                ReportSearchBar(
                    searchQuery = reportState.searchQuery.value,
                    onSearchChange = { query ->
                        reportState.searchQuery.value = query
                        reportState.pagination.resetPage()
                    },
                    onAddMenuClick = { uiState.addMenuExpanded.value = !uiState.addMenuExpanded.value },
                    addMenuExpanded = uiState.addMenuExpanded.value,
                    onAddMenuDismiss = { uiState.addMenuExpanded.value = false },
                    addMenuContent = { AddMenu() },
                    onConfigurationClick = { uiState.configurationDialogVisible.value = true }
                )

                LazyColumn(
                    state = uiState.lazyListState,
                    modifier = Modifier.weight(1f)
                ) {

                    // Tabela z danymi
                    reportTable(reportHandler, reportState, reportState.data)
                }
            }
        }

        // Dialog konfiguracji
        if (uiState.configurationDialogVisible.value) {
            ReportConfigurationDialog(
                reportName = reportHandler.getReportName(),
                reportState = reportState,
                onDismiss = { uiState.configurationDialogVisible.value = false },
            )
        }
    }

    @Composable
    private fun rememberReportUIState(): ReportUIState {
        return ReportUIState(
            lazyListState = rememberLazyListState(),
            coroutineScope = rememberCoroutineScope(),
            addMenuExpanded = remember { mutableStateOf(false) },
            configurationDialogVisible = remember { mutableStateOf(false) }
        )
    }

    private data class ReportUIState(
        val lazyListState: LazyListState,
        val coroutineScope: CoroutineScope,
        val addMenuExpanded: MutableState<Boolean>,
        val configurationDialogVisible: MutableState<Boolean>
    )

    @Composable
    protected open fun AddMenu() {
        Box {}
    }
}