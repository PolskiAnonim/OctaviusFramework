package org.octavius.report.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.ColumnWidth
import org.octavius.report.component.LocalReportHandler
import org.octavius.report.component.ReportState

@Composable
fun ReportHeaderRow(
    reportState: ReportState,
    visibleColumns: List<String>
) {
    val reportHandler = LocalReportHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 8.dp)
    ) {
        visibleColumns.forEachIndexed { index, key ->
            val column = reportHandler.reportStructure.getColumn(key)

            val cellModifier = when (val colWidth = column.width) {
                is ColumnWidth.Fixed -> Modifier.width(colWidth.width)
                is ColumnWidth.Flexible -> Modifier.weight(colWidth.weight)
            }

            column.RenderHeader(reportHandler::onEvent, key, reportState, cellModifier)

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