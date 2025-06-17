package org.octavius.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.navigator.Screen
import org.octavius.report.FilterPanel
import org.octavius.report.Report
import org.octavius.report.ReportState

abstract class ReportScreen : Screen {

    abstract val report: Report

    private val reportState = ReportState()

    @Composable
    protected open fun AddMenu() {
        Box {}
    }

    @Composable
    override fun Content(paddingValues: PaddingValues) {
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var showFilters by remember { mutableStateOf(false) }
        var dataList by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }

        // Dla śledzenia zmian filtrów
        val filteringState = derivedStateOf {
            report.getColumnStates().map { (key, state) ->
                key to (state.filtering.value?.dirtyState?.value)
            }.toMap()
        }

        // Efekt pobierający dane po zmianie parametrów
        LaunchedEffect(
            reportState.currentPage.value,
            reportState.searchQuery.value,
            reportState.pageSize.value,
            filteringState.value
        ) {
            report.fetchData(
                page = reportState.currentPage.value,
                searchQuery = reportState.searchQuery.value,
                pageSize = reportState.pageSize.value
            ) { result, pages ->
                dataList = result
                reportState.totalPages.value = pages.toInt()
            }
        }

        Column(modifier = Modifier.Companion.padding(paddingValues)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.Companion.weight(1f)
            ) {
                item {
                    // Pasek wyszukiwania i filtrowania
                    Row(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Companion.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = reportState.searchQuery.value,
                            onValueChange = {
                                reportState.searchQuery.value = it
                                reportState.currentPage.value = 0
                            },
                            modifier = Modifier.Companion.weight(1f),
                            placeholder = { Text("Szukaj...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Szukaj"
                                )
                            },
                            trailingIcon = {
                                if (reportState.searchQuery.value.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            reportState.searchQuery.value = ""
                                            reportState.currentPage.value = 0
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Wyczyść"
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Menu dodawania
                        var expanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier.Companion.padding(start = 8.dp)
                        ) {
                            IconButton(
                                onClick = { expanded = !expanded }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Input,
                                    contentDescription = "Dodaj"
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                AddMenu()
                            }
                        }

                        // Przycisk filtrów z aktywnym wskaźnikiem
                        IconButton(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier.Companion.padding(start = 8.dp)
                        ) {
                            if (report.areAnyFiltersActive()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterListOff,
                                        contentDescription = "Filtry aktywne",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtry"
                                )
                            }
                        }
                    }

                    // Panel filtrów
                    if (showFilters) {
                        FilterPanel(
                            columns = report.getColumns().filter { it.value.filterable },
                            columnStates = report.getColumnStates().filter { it.value.filtering.value != null },
                            onPageReset = {
                                reportState.currentPage.value = 0
                            }
                        )
                    }
                }

                item {
                    // Nagłówki kolumn
                    Row(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 8.dp)
                    ) {
                        report.getColumns().forEach { (key, column) ->
                            Box(
                                modifier = Modifier.Companion
                                    .weight(column.width)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Companion.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Companion.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = column.header,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Companion.Center
                                    )

                                    // Wskaźnik aktywnego filtra
                                    val columnState = report.getColumnStates()[key]
                                    if (column.filterable && columnState!!.filtering.value?.isActive() == true) {
                                        Icon(
                                            imageVector = Icons.Default.FilterAlt,
                                            contentDescription = "Filtr aktywny",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.Companion.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(dataList) { rowData ->
                    Row(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .run {
                                if (report.onRowClick != null) {
                                    this.clickable { report.onRowClick!!.invoke(rowData) }
                                } else {
                                    this
                                }
                            }
                    ) {
                        report.getColumns().forEach { (_, column) ->
                            Box(
                                modifier = Modifier.Companion
                                    .weight(column.width)
                                    .padding(horizontal = 4.dp)
                            ) {
                                column.RenderCell(rowData[column.fieldName], Modifier.Companion)
                            }
                        }
                    }

                    // Separator
                    Spacer(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
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
    }
}