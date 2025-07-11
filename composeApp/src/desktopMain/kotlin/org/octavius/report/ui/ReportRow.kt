package org.octavius.report.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.special.SpecialColumn

@Composable
fun ReportRow(
    rowData: Map<String, Any?>,
    visibleColumns: List<String>,
    allColumns: Map<String, ReportColumn>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(vertical = 4.dp)
    ) {
        visibleColumns.forEachIndexed { index, key ->
            val column = allColumns[key]!!

            val cellModifier = when (val colWidth = column.width) {
                is ColumnWidth.Fixed -> Modifier.width(colWidth.width)
                is ColumnWidth.Flexible -> Modifier.weight(colWidth.weight)
            }

            Box(
                modifier = cellModifier,
                contentAlignment = Alignment.CenterStart
            ) {
                // Dla SpecialColumn, 'item' to cała mapa. Dla innych - konkretna wartość.
                val cellData = if (column is SpecialColumn) {
                    rowData
                } else {
                    rowData[column.databaseColumnName]
                }
                column.RenderCell(cellData, Modifier)
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