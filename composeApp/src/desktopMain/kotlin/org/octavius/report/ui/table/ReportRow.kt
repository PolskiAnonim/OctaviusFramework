package org.octavius.report.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.ColumnWidth
import org.octavius.report.LocalReportContext
import org.octavius.report.column.type.special.SpecialColumn

@Composable
fun ReportRow(
    rowData: Map<String, Any?>,
    visibleColumns: List<String>,
    modifier: Modifier = Modifier
) {
    val reportContext = LocalReportContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(vertical = 4.dp)
    ) {
        visibleColumns.forEachIndexed { index, key ->
            val column = reportContext.reportStructure.getColumn(key)

            val cellModifier = when (val colWidth = column.width) {
                is ColumnWidth.Fixed -> Modifier.width(colWidth.width)
                is ColumnWidth.Flexible -> Modifier.weight(colWidth.weight)
            }

            Box(
                modifier = cellModifier,
                contentAlignment = Alignment.CenterStart
            ) {
                // Dla SpecialColumn, przekazywany jest dodatkowy kontekst
                if (column is SpecialColumn) {
                    column.RenderCell(rowData, reportContext.reportState, reportContext.onEvent, modifier)
                } else {
                    column.RenderCell(rowData[key], Modifier)
                }
            }

            // Separator między kolumnami (oprócz ostatniej)
            if (index < visibleColumns.size - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}