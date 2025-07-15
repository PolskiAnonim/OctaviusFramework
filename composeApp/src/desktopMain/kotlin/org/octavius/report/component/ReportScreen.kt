package org.octavius.report.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.octavius.navigator.Screen
import org.octavius.report.ReportEvent
import org.octavius.report.configuration.ReportConfigurationDialog
import org.octavius.report.management.ColumnManagementPanel
import org.octavius.report.ui.PaginationComponent
import org.octavius.report.ui.ReportSearchBar
import org.octavius.report.ui.table.ReportTable

abstract class ReportScreen : Screen {
    abstract override val title: String

    abstract fun createReportStructure(): ReportStructureBuilder


    @Composable
    private fun rememberReportHandler(): ReportHandler {
        val scope = rememberCoroutineScope()
        val reportStructure = remember { createReportStructure().build() }

        return remember(scope, reportStructure) {
            ReportHandler(
                coroutineScope = scope,
                reportStructure = reportStructure
            )
        }
    }

    @Composable
    override fun Content() {
        val reportHandler: ReportHandler = rememberReportHandler()
        val state by reportHandler.state.collectAsState()
        val uiState = rememberReportUIState()

        // Przy zmianie danych
        LaunchedEffect(state.data) {
            // Przewiń listę na samą górę (do elementu o indeksie 0)
            uiState.lazyListState.animateScrollToItem(0)
        }

        CompositionLocalProvider(LocalReportHandler provides reportHandler) {
            Scaffold(
                topBar = {
                    ColumnManagementPanel(
                        reportState = state,
                        modifier = Modifier.padding(8.dp)
                    )
                },
                bottomBar = {
                    PaginationComponent(state.pagination, onEvent = reportHandler::onEvent)
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    // Pasek wyszukiwania
                    ReportSearchBar(
                        searchQuery = state.searchQuery,
                        onSearchChange = { query ->
                            reportHandler.onEvent(ReportEvent.SearchQueryChanged(query))
                        },
                        onAddMenuClick = { uiState.addMenuExpanded.value = !uiState.addMenuExpanded.value },
                        addMenuExpanded = uiState.addMenuExpanded.value,
                        onAddMenuDismiss = { uiState.addMenuExpanded.value = false },
                        addMenuContent = { AddMenu() },
                        onConfigurationClick = { uiState.configurationDialogVisible.value = true }
                    )
                    ReportTable(state, uiState.lazyListState)
                }
            }
        }

        // Dialog konfiguracji
        if (uiState.configurationDialogVisible.value) {
            ReportConfigurationDialog(
                onEvent = reportHandler::onEvent,
                reportName = reportHandler.reportStructure.reportName,
                reportState = reportHandler.state.value,
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