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
    columns: List<ReportColumn>,
    reportState: ReportState
) {
    if (columns.isEmpty()) return

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

                if (reportState.filtering.isNotEmpty()) {
                    Button(
                        onClick = {
                            reportState.filtering.clear()
                            reportState.currentPage.value = 1
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
                    columns.forEachIndexed { index, column ->
                        Tab(
                            selected = selectedColumnIndex == index,
                            onClick = { selectedColumnIndex = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(column.header)

                                    // Dodaj wskaźnik aktywnego filtra
                                    if (reportState.filtering.containsKey(column.name)) {
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

            // Wyświetlanie interfejsu filtrowania dla wybranej kolumny
            val selectedColumn = columns.getOrNull(selectedColumnIndex)
            selectedColumn?.let { column ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(scrollState)
                ) {
                    column.RenderFilter(
                        currentFilter = reportState.filtering[column.name],
                        onFilterChanged = { filterValue ->
                            if (filterValue != null) {
                                reportState.filtering[column.name] = filterValue
                            } else {
                                reportState.filtering.remove(column.name)
                            }
                            reportState.currentPage.value = 1
                        }
                    )
                }

                if (reportState.filtering.containsKey(column.name)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                reportState.filtering.remove(column.name)
                                reportState.currentPage.value = 1
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Wyczyść filtr"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wyczyść filtr dla ${column.header}")
                        }
                    }
                }
            }
        }
    }
}