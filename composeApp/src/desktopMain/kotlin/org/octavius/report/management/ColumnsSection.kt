package org.octavius.report.management

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.LocalReportContext
import org.octavius.report.ReportEvent
import org.octavius.report.component.ReportState
import org.octavius.ui.component.ChipConstants
import org.octavius.ui.component.DraggableChip

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnsSection() {
    val reportContext = LocalReportContext.current
    val allColumns = reportContext.reportState.columnKeysOrder
    val visibleColumns = reportContext.reportState.visibleColumns
    val onDragEnd = { fromIndex: Int, toIndex: Int ->
        if (fromIndex != toIndex) {
            val columnKeysOrder = reportContext.reportState.columnKeysOrder.toMutableList()
            val item = columnKeysOrder.removeAt(fromIndex)
            columnKeysOrder.add(toIndex, item)
            reportContext.onEvent.invoke(ReportEvent.ColumnOrderChanged(columnKeysOrder))
        }
    }
    Column {
        Text(
            text = Translations.get("report.columns.columns"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Iterujemy po aktualnej kolejności WSZYSTKICH kolumn, ale filtrujemy te,
            // które nie są zarządzalne
            items(allColumns.filter { reportContext.reportStructure.manageableColumnKeys.contains(it) }) { columnKey ->
                val isVisible = visibleColumns.contains(columnKey)
                val index = allColumns.indexOf(columnKey)
                DraggableChip(
                    text = reportContext.reportStructure.getColumn(columnKey).header,
                    dragData = "$columnKey:$index",
                    backgroundColor = if (isVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.8f
                    ),
                    textColor = if (isVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.8f
                    ),
                    modifier = if (!isVisible) {
                        Modifier.border(
                            width = ChipConstants.borderWidth,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = ChipConstants.chipShape
                        )
                    } else Modifier,
                    onDrop = { transferData ->
                        val parts = transferData.split(":")
                        if (parts.size == 2) {
                            val draggedColumnKey = parts[0]
                            val fromIndex = parts[1].toIntOrNull() ?: return@DraggableChip false

                            if (draggedColumnKey != columnKey) {
                                onDragEnd(fromIndex, index)
                                return@DraggableChip true
                            }
                        }
                        false
                    },
                    leadingIcon = {
                        Box(modifier = Modifier.clickable {
                            toggleColumnVisibility(
                                reportContext.onEvent,
                                reportContext.reportState,
                                columnKey,
                                isVisible
                            )
                        }) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) Translations.get("report.columns.hideColumn") else Translations.get(
                                    "report.columns.showColumn"
                                ),
                                tint = if (isVisible) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun toggleColumnVisibility(
    onEvent: (ReportEvent) -> Unit,
    reportState: ReportState,
    columnKey: String,
    isVisible: Boolean
) {
    if (isVisible) {
        val visibleColumns = reportState.visibleColumns.toMutableSet()
        visibleColumns.remove(columnKey)
        onEvent.invoke(ReportEvent.ColumnVisibilityChanged(visibleColumns.toSet()))
    } else {
        val visibleColumns = reportState.visibleColumns.toMutableSet()
        visibleColumns.add(columnKey)
        onEvent.invoke(ReportEvent.ColumnVisibilityChanged(visibleColumns.toSet()))
    }
}