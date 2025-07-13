package org.octavius.report.management

import androidx.compose.foundation.*
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.domain.SortDirection
import org.octavius.localization.Translations
import org.octavius.report.ReportEvent
import org.octavius.report.component.ReportState
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

private object ChipConstants {
    val chipShape = RoundedCornerShape(16.dp)
    val horizontalPadding = 12.dp
    val verticalPadding = 6.dp
    val iconSpacing = 4.dp
    val dropZoneHeight = 40.dp
    val borderWidth = 1.dp
    val dropZoneBorderWidth = 2.dp
}

@Composable
fun ColumnManagementPanel(
    onEvent: (ReportEvent) -> Unit,
    reportState: ReportState,
    manageableColumnKeys: List<String>,
    columnNames: Map<String, String>,
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
                        contentDescription = if (isExpanded) Translations.get("expandable.collapse") else Translations.get("expandable.expand")
                    )
                }

            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Sekcja widocznych kolumn z możliwością sortowania
                ColumnsSection(onEvent, manageableColumnKeys = manageableColumnKeys, columnNames = columnNames, reportState = reportState)

                Spacer(modifier = Modifier.height(16.dp))

                // Sekcja sortowania
                SortingSection(onEvent, reportState = reportState, columnNames = columnNames)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColumnsSection(
    onEvent: (ReportEvent) -> Unit,
    manageableColumnKeys: List<String>,
    columnNames: Map<String, String>,
    reportState: ReportState
) {
    val allColumns = reportState.columnKeysOrder
    val visibleColumns = reportState.visibleColumns
    val onDragEnd = { fromIndex: Int, toIndex: Int ->
        if (fromIndex != toIndex) {
            val columnKeysOrder = reportState.columnKeysOrder.toMutableList()
            val item = columnKeysOrder.removeAt(fromIndex)
            columnKeysOrder.add(toIndex, item)
            onEvent.invoke(ReportEvent.ColumnOrderChanged(columnKeysOrder))
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
            items(allColumns.filter { manageableColumnKeys.contains(it) }) { columnKey ->
                val isVisible = visibleColumns.contains(columnKey)
                val index = allColumns.indexOf(columnKey)
                DraggableChip(
                    text = columnNames[columnKey]!!,
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
                                onEvent,
                                reportState,
                                columnKey,
                                isVisible
                            )
                        }) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isVisible) Translations.get("report.columns.hideColumn") else Translations.get("report.columns.showColumn"),
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
private fun SortingSection(
    onEvent: (ReportEvent) -> Unit,
    reportState: ReportState,
    columnNames: Map<String, String>
) {
    // Pobierz kolumny z report state (fallback)
    val allColumns = reportState.columnKeysOrder
    Column {
        Text(
            text = Translations.get("report.columns.sorting"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Aktualne sortowania
        val currentSort = reportState.sortOrder
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
                        text = columnNames[columnKey]!!,
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
                                        reorderSortColumns(onEvent, reportState, fromIndex, sortIndex)
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
                                        onEvent,
                                        reportState,
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
                                modifier = Modifier.clickable { removeSortColumn(onEvent,reportState, columnKey) }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Strefa docelowa dla przeciągania kolumn do sortowania
        Text(
            text = Translations.get("report.columns.sortZone"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = createSortDropTarget(onEvent, allColumns, reportState)
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = ChipConstants.dropZoneBorderWidth,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
                .height(ChipConstants.dropZoneHeight)
        ) {
            if (currentSort.isEmpty()) {
                Text(
                    text = Translations.get("report.columns.dragColumnsHere"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = Translations.get("report.columns.dragMoreColumns"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun DraggableChip(
    text: String,
    dragData: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onDrop: (String) -> Boolean = { false },
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val dragHandleInteractionSource = remember { MutableInteractionSource() }
    val isDragHandleHovered by dragHandleInteractionSource.collectIsHoveredAsState()

    Surface(
        shape = ChipConstants.chipShape,
        color = backgroundColor,
        modifier = modifier
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val transferData = extractTransferData(event) ?: return false
                        return onDrop(transferData)
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ChipConstants.horizontalPadding,
                vertical = ChipConstants.verticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ChipConstants.iconSpacing)
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = Translations.get("report.columns.dragToReorder"),
                tint = if (isDragHandleHovered) MaterialTheme.colorScheme.primary else textColor,
                modifier = Modifier
                    .hoverable(dragHandleInteractionSource)
                    .dragAndDropSource {
                        DragAndDropTransferData(
                            transferable = DragAndDropTransferable(StringSelection(dragData)),
                            supportedActions = listOf(DragAndDropTransferAction.Move, DragAndDropTransferAction.Copy)
                        )
                    }
            )

            leadingIcon?.invoke()

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )

            trailingIcon?.invoke()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun extractTransferData(event: DragAndDropEvent): String? {
    return event.awtTransferable.let { transferable ->
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            transferable.getTransferData(DataFlavor.stringFlavor) as String
        } else {
            null
        }
    }
}

private fun createSortDropTarget(
    onEvent: (ReportEvent) -> Unit,
    allColumns: List<String>,
    reportState: ReportState
): DragAndDropTarget {
    return object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val transferData = extractTransferData(event) ?: return false

            val columnKey = if (transferData.contains(":")) {
                transferData.split(":")[0]
            } else {
                transferData
            }

            val currentSortValue = reportState.sortOrder
            if (allColumns.contains(columnKey) && !currentSortValue.any { it.first == columnKey }) {
                val newSort = currentSortValue.toMutableList()
                newSort.add(columnKey to SortDirection.Ascending)
                onEvent.invoke(ReportEvent.SortOrderChanged(newSort))
                return true
            }
            return false
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