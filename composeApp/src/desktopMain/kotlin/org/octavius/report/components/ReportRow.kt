package org.octavius.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.column.ReportColumn

@Composable
fun ReportRow(
    rowData: Map<String, Any?>,
    visibleColumns: List<String>,
    allColumns: Map<String, ReportColumn>,
    onRowClick: ((Map<String, Any?>) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(vertical = 4.dp)
            .run {
                if (onRowClick != null) {
                    this.clickable { onRowClick.invoke(rowData) }
                } else {
                    this
                }
            }
    ) {
        visibleColumns.forEachIndexed { index, key ->
            val column = allColumns[key]
            if (column != null) {
                Box(
                    modifier = Modifier
                        .weight(column.width)
                        .padding(horizontal = 4.dp)
                ) {
                    column.RenderCell(rowData[column.fieldName], Modifier)
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
}