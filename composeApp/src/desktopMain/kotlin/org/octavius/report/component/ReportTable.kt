package org.octavius.report.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun LazyListScope.reportTable(
    reportHandler: ReportHandler,
    reportState: ReportState,
    dataList: List<Map<String, Any?>>
) {
    // Nagłówki kolumn
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 8.dp)
        ) {
            val visibleColumns = reportState.columnKeys.filter { 
                reportState.visibleColumns.value.contains(it) 
            }
            val allColumns = reportHandler.getColumns()
            val filters = reportHandler.getFilters()
            
            visibleColumns.forEachIndexed { index, key ->
                val column = allColumns[key]
                if (column != null) {
                    ReportColumnHeader(
                        columnKey = key,
                        column = column,
                        filter = filters[key],
                        filterData = reportState.filterValues.value[key],
                        reportState = reportState,
                        modifier = Modifier.weight(column.width)
                    )

                    // Separator między kolumnami w nagłówku (oprócz ostatniej)
                    if (index < visibleColumns.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }

    // Wiersze danych
    items(dataList.size) { index ->
        val rowData = dataList[index]
        val visibleColumns = reportState.columnKeys.filter { 
            reportState.visibleColumns.value.contains(it) 
        }
        
        ReportRow(
            rowData = rowData,
            visibleColumns = visibleColumns,
            allColumns = reportHandler.getColumns(),
            onRowClick = reportHandler.onRowClick
        )

        // Separator
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}