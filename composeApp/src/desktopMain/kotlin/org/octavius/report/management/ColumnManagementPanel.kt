package org.octavius.report.management

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.domain.SortDirection
import org.octavius.localization.Translations
import org.octavius.report.LocalReportContext
import org.octavius.report.ReportEvent
import org.octavius.report.component.ReportState
import org.octavius.ui.component.ChipConstants
import org.octavius.ui.component.DraggableChip
import org.octavius.ui.component.DropZone
import org.octavius.ui.component.DropZoneConstants
import org.octavius.util.extractTransferData

@Composable
fun ColumnManagementPanel(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header z przyciskiem rozwijania
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { isExpanded = !isExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = Translations.get("report.management.columnManagement"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isExpanded) Translations.get("expandable.collapse") else Translations.get(
                            "expandable.expand"
                        )
                    )
                }

            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Sekcja widocznych kolumn z możliwością sortowania
                ColumnsSection()

                Spacer(modifier = Modifier.height(16.dp))

                // Sekcja sortowania
                SortingSection()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColumnsSection() {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SortingSection() {
    val reportContext = LocalReportContext.current

    Column {
        Text(
            text = Translations.get("report.columns.sorting"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Aktualne sortowania
        val currentSort = reportContext.reportState.sortOrder
        if (currentSort.isNotEmpty()) {
            Text(
                text = Translations.get("report.columns.activeSorting"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentSort) { (columnKey, direction) ->
                    val sortIndex = currentSort.indexOfFirst { it.first == columnKey }
                    DraggableChip(
                        text = reportContext.reportStructure.getColumn(columnKey).header,
                        dragData = "SORT:$columnKey:$sortIndex",
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onDrop = { transferData ->
                            if (transferData.startsWith("SORT:")) {
                                val parts = transferData.removePrefix("SORT:").split(":")
                                if (parts.size == 2) {
                                    val draggedColumnKey = parts[0]
                                    val fromIndex = parts[1].toIntOrNull() ?: return@DraggableChip false

                                    if (draggedColumnKey != columnKey) {
                                        reorderSortColumns(reportContext.onEvent, reportContext.reportState, fromIndex, sortIndex)
                                        return@DraggableChip true
                                    }
                                }
                            }
                            false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (direction) {
                                    SortDirection.Ascending -> Icons.Default.ArrowUpward
                                    SortDirection.Descending -> Icons.Default.ArrowDownward
                                },
                                contentDescription = when (direction) {
                                    SortDirection.Ascending -> Translations.get("report.columns.ascending")
                                    SortDirection.Descending -> Translations.get("report.columns.descending")
                                },
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.clickable {
                                    updateSortDirection(
                                        reportContext.onEvent,
                                        reportContext.reportState,
                                        columnKey,
                                        if (direction == SortDirection.Ascending) SortDirection.Descending else SortDirection.Ascending
                                    )
                                }
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "×",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.clickable { removeSortColumn(reportContext.onEvent, reportContext.reportState, columnKey) }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        DropZone(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(DropZoneConstants.dropZoneHeight),
            onDrop = { event ->
                val transferData = extractTransferData(event) ?: return@DropZone false

                val columnKey = if (transferData.contains(":")) {
                    transferData.split(":")[0]
                } else {
                    transferData
                }

                val currentSort = reportContext.reportState.sortOrder
                if (!currentSort.any { it.first == columnKey }) {
                    val newSort = reportContext.reportState.sortOrder.toMutableList()
                    newSort.add(columnKey to SortDirection.Ascending)
                    reportContext.onEvent.invoke(ReportEvent.SortOrderChanged(newSort))
                    return@DropZone true
                }
                return@DropZone false
            }
        ) { isHovered ->
            val textKey = if (reportContext.reportState.sortOrder.isEmpty()) {
                "report.columns.dragColumnsHere"
            } else {
                "report.columns.dragMoreColumns"
            }

            Text(
                text = Translations.get(textKey),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isHovered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
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

private fun updateSortDirection(
    onEvent: (ReportEvent) -> Unit,
    reportState: ReportState,
    columnKey: String,
    newDirection: SortDirection
) {
    val currentSort = reportState.sortOrder.toMutableList()
    val index = currentSort.indexOfFirst { it.first == columnKey }
    if (index >= 0) {
        currentSort[index] = columnKey to newDirection
        onEvent.invoke(ReportEvent.SortOrderChanged(currentSort))
    }
}

private fun removeSortColumn(
    onEvent: (ReportEvent) -> Unit,
    reportState: ReportState,
    columnKey: String
) {
    val newSort = reportState.sortOrder.filter { it.first != columnKey }
    onEvent.invoke(ReportEvent.SortOrderChanged(newSort))
}

private fun reorderSortColumns(
    onEvent: (ReportEvent) -> Unit,
    reportState: ReportState,
    fromIndex: Int,
    toIndex: Int,
) {
    val newSort = reportState.sortOrder.toMutableList()
    val item = newSort.removeAt(fromIndex)
    newSort.add(toIndex, item)
    onEvent.invoke(ReportEvent.SortOrderChanged(newSort))
}