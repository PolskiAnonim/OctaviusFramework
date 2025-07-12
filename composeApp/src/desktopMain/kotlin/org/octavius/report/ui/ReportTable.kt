package org.octavius.report.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.ColumnWidth
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportState

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
            val allColumns = reportHandler.reportStructure.getAllColumns()

            visibleColumns.forEachIndexed { index, key ->
                val column = allColumns[key]!!

                val cellModifier = when (val colWidth = column.width) {
                    is ColumnWidth.Fixed -> Modifier.width(colWidth.width)
                    is ColumnWidth.Flexible -> Modifier.weight(colWidth.weight)
                }

                column.RenderHeader(key, reportState, cellModifier)

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

    // Wiersze danych
    items(dataList.size) { index ->
        val rowData = dataList[index]
        val visibleColumns = reportState.columnKeys.filter { 
            reportState.visibleColumns.value.contains(it) 
        }

        ReportRow(
            rowData = rowData,
            visibleColumns = visibleColumns,
            allColumns = reportHandler.reportStructure.getAllColumns()
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