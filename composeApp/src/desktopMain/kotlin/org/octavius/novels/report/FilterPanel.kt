package org.octavius.novels.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.column.ReportColumn

@Composable
fun FilterPanel(
    columns: Map<String, ReportColumn>,
    columnStates: Map<String, ColumnState>,
    onPageReset: () -> Unit
) {
    if (columns.isEmpty()) return

    val columnsList = columns.entries.toList()
    var selectedColumnIndex by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Nagłówek panelu filtrów
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtry",
                    style = MaterialTheme.typography.titleLarge
                )

                // Przycisk resetowania wszystkich filtrów
                val anyFilterActive = columnStates.values.any {
                    it.filtering.value?.isActive() == true
                }

                if (anyFilterActive) {
                    Button(
                        onClick = {
                            columnStates.values.forEach { state ->
                                state.filtering.value?.reset()
                            }
                            onPageReset()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Wyczyść wszystkie filtry"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Wyczyść filtry")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wybór kolumny do filtrowania
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kolumna:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // TabRow z nazwami kolumn
                ScrollableTabRow(
                    selectedTabIndex = selectedColumnIndex,
                    modifier = Modifier.weight(1f)
                ) {
                    columnsList.forEachIndexed { index, (key, column) ->
                        val state = columnStates[key]
                        val isFilterActive = state?.filtering?.value?.isActive() == true

                        Tab(
                            selected = selectedColumnIndex == index,
                            onClick = { selectedColumnIndex = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(column.header)

                                    // Wskaźnik aktywnego filtra
                                    if (isFilterActive) {
                                        Badge(
                                            modifier = Modifier.padding(start = 4.dp),
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text("F")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interfejs filtrowania dla wybranej kolumny
            if (selectedColumnIndex < columnsList.size) {
                val (columnKey, selectedColumn) = columnsList[selectedColumnIndex]
                val state = columnStates[columnKey]
                val filter = state?.filtering?.value

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(scrollState)
                ) {
                    if (filter != null) {
                        selectedColumn.RenderFilter(
                            currentFilter = filter,
                            onFilterChanged = { newFilter ->
                                // Tutaj nic nie robimy - filtr jest już MutableState
                                // i modyfikacja jest obsługiwana wewnątrz RenderFilter
                                onPageReset()
                            }
                        )
                    }
                }

                // Przycisk czyszczenia aktywnego filtra
                if (filter?.isActive() == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                filter.reset()
                                onPageReset()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Wyczyść filtr"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wyczyść filtr dla ${selectedColumn.header}")
                        }
                    }
                }
            }
        }
    }
}