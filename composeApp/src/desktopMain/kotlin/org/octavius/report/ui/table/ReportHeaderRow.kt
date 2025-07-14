package org.octavius.report.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.ColumnWidth
import org.octavius.report.LocalReportContext

@Composable
fun ReportHeaderRow(
    visibleColumns: List<String>
) {
    val reportContext = LocalReportContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 8.dp)
    ) {
        visibleColumns.forEachIndexed { index, key ->
            val column = reportContext.reportStructure.getColumn(key)

            val cellModifier = when (val colWidth = column.width) {
                is ColumnWidth.Fixed -> Modifier.width(colWidth.width)
                is ColumnWidth.Flexible -> Modifier.weight(colWidth.weight)
            }

            // Przekazujemy `onEvent` i `reportState` z kontekstu
            column.RenderHeader(reportContext.onEvent, key, reportContext.reportState, cellModifier)

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