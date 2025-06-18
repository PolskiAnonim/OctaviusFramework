package org.octavius.report.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.report.ReportState
import org.octavius.report.SortDirection
import java.awt.datatransfer.StringSelection

@Composable
fun ColumnManagementPanel(
    columnNames: Map<String, String>,
    reportState: ReportState,
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
                        text = "Zarządzanie kolumnami",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isExpanded) "Zwiń" else "Rozwiń"
                    )
                }

            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sekcja widocznych kolumn z możliwością sortowania
                ColumnsSection(columnNames = columnNames, reportState = reportState)
                    
                Spacer(modifier = Modifier.height(16.dp))

                // Sekcja sortowania
                SortingSection(reportState = reportState, columnNames = columnNames)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColumnsSection(
    columnNames: Map<String, String>,
    reportState: ReportState
) {
    val allColumns = reportState.columnKeys
    val visibleColumns = reportState.visibleColumns
    
    Column {
        Text(
            text = "Kolumny",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allColumns) { columnKey ->
                val isVisible = visibleColumns.value.contains(columnKey)
                SimpleColumnChip(
                    columnNames[columnKey]!!,
                    columnKey = columnKey,
                    index = allColumns.indexOf(columnKey),
                    isVisible = isVisible,
                    onVisibilityToggle = {
                        if (isVisible) {
                            reportState.visibleColumns.value = reportState.visibleColumns.value.filter { it != columnKey }.toSet()
                        } else {
                            val mutableSet = reportState.visibleColumns.value.toMutableSet()
                            mutableSet.add(columnKey)
                            reportState.visibleColumns.value = mutableSet.toSet()
                        }
                    },
                    onDragEnd = { fromIndex, toIndex ->
                        if (fromIndex != toIndex && fromIndex >= 0 && toIndex >= 0) {
                            val item = reportState.columnKeys.removeAt(fromIndex)
                            reportState.columnKeys.add(toIndex, item)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun SimpleColumnChip(
    columnName: String,
    columnKey: String,
    index: Int,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    onDragEnd: ((Int, Int) -> Unit)? = null
) {
    val dragHandleInteractionSource = remember { MutableInteractionSource() }
    val isDragHandleHovered by dragHandleInteractionSource.collectIsHoveredAsState()
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isVisible -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        },
        modifier = Modifier
            .let { modifier ->
                if (!isVisible) {
                    modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    modifier
                }
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val transferData = event.awtTransferable.let { 
                            if (it.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                it.getTransferData(DataFlavor.stringFlavor) as String
                            } else {
                                return false
                            }
                        }
                        
                        val parts = transferData.split(":")
                        if (parts.size == 2) {
                            val draggedColumnKey = parts[0]
                            val fromIndex = parts[1].toIntOrNull() ?: return false
                            
                            if (draggedColumnKey != columnKey && onDragEnd != null) {
                                onDragEnd(fromIndex, index)
                                return true
                            }
                        }
                        return false
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Uchwyt przeciągania
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Przeciągnij aby zmienić kolejność",
                tint = if (isDragHandleHovered) {
                    MaterialTheme.colorScheme.primary
                } else if (isVisible) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .hoverable(dragHandleInteractionSource)
                    .dragAndDropSource {
                        DragAndDropTransferData(
                            transferable = DragAndDropTransferable(StringSelection("$columnKey:$index")),
                            supportedActions = listOf(DragAndDropTransferAction.Move, DragAndDropTransferAction.Copy)
                        )
                    }
            )
            // Przycisk widoczności
            Box(
                modifier = Modifier.clickable { onVisibilityToggle() }
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isVisible) "Ukryj kolumnę" else "Pokaż kolumnę",
                    tint = if (isVisible) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Nazwa kolumny
            Text(
                text = columnName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isVisible) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                },
                modifier = Modifier
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SortingSection(
    reportState: ReportState,
    columnNames: Map<String, String>
) {
    // Pobierz kolumny z report state (fallback)
    val allColumns = reportState.columnKeys
    Column {
        Text(
            text = "Sortowanie",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Aktualne sortowania
        val currentSort = reportState.sortOrder.value
        if (currentSort.isNotEmpty()) {
            Text(
                text = "Aktywne sortowania:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentSort) { (columnKey, direction) ->
                    val sortIndex = currentSort.indexOfFirst { it.first == columnKey }
                    SortChip(
                        columnName = columnNames[columnKey] ?: columnKey,
                        columnKey = columnKey,
                        direction = direction,
                        sortIndex = sortIndex,
                        onDirectionChange = { newDirection ->
                            val newSort = currentSort.toMutableList()
                            val index = newSort.indexOfFirst { it.first == columnKey }
                            if (index >= 0) {
                                newSort[index] = columnKey to newDirection
                                reportState.sortOrder.value = newSort
                                reportState.currentPage.value = 0
                            }
                        },
                        onRemove = {
                            val newSort = currentSort.filter { it.first != columnKey }
                            reportState.sortOrder.value = newSort
                            reportState.currentPage.value = 0
                        },
                        onDragEnd = { fromIndex, toIndex ->
                            if (fromIndex != toIndex && fromIndex >= 0 && toIndex >= 0 && 
                                fromIndex < currentSort.size && toIndex < currentSort.size) {
                                val newSort = currentSort.toMutableList()
                                val item = newSort.removeAt(fromIndex)
                                newSort.add(toIndex, item)
                                reportState.sortOrder.value = newSort
                                reportState.currentPage.value = 0
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Strefa docelowa dla przeciągania kolumn do sortowania
        Text(
            text = "Strefa sortowania (przeciągnij kolumny tutaj):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = object : DragAndDropTarget {
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val transferData = event.awtTransferable.let { 
                                if (it.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                    it.getTransferData(DataFlavor.stringFlavor) as String
                                } else {
                                    return false
                                }
                            }
                            
                            // Handle transfer data - może być "columnKey" lub "columnKey:index"
                            val columnKey = if (transferData.contains(":")) {
                                transferData.split(":")[0]
                            } else {
                                transferData
                            }
                            
                            // Sprawdź czy kolumna istnieje i nie jest już posortowana
                            val currentSortValue = reportState.sortOrder.value
                            if (allColumns.contains(columnKey) && !currentSortValue.any { it.first == columnKey }) {
                                val newSort = currentSortValue.toMutableList()
                                println("Before: $newSort")
                                newSort.add(columnKey to SortDirection.ASC)
                                println("After: $newSort")
                                reportState.sortOrder.value = newSort
                                reportState.currentPage.value = 0
                                return true
                            }
                            return false
                        }
                    }
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
                .height(40.dp)
        ) {
            if (currentSort.isEmpty()) {
                Text(
                    text = "Przeciągnij kolumny tutaj aby dodać sortowanie",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = "Przeciągnij więcej kolumn aby dodać dodatkowe sortowanie",
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
private fun SortChip(
    columnName: String,
    columnKey: String,
    direction: SortDirection,
    sortIndex: Int,
    onDirectionChange: (SortDirection) -> Unit,
    onRemove: () -> Unit,
    onDragEnd: ((Int, Int) -> Unit)? = null
) {
    val dragHandleInteractionSource = remember { MutableInteractionSource() }
    val isDragHandleHovered by dragHandleInteractionSource.collectIsHoveredAsState()
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val transferData = event.awtTransferable.let { 
                            if (it.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                it.getTransferData(DataFlavor.stringFlavor) as String
                            } else {
                                return false
                            }
                        }
                        
                        if (transferData.startsWith("SORT:")) {
                            val parts = transferData.removePrefix("SORT:").split(":")
                            if (parts.size == 2) {
                                val draggedColumnKey = parts[0]
                                val fromIndex = parts[1].toIntOrNull() ?: return false
                                
                                if (draggedColumnKey != columnKey && onDragEnd != null) {
                                    onDragEnd(fromIndex, sortIndex)
                                    return true
                                }
                            }
                        }
                        return false
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Uchwyt przeciągania
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Przeciągnij aby zmienić kolejność sortowania",
                tint = if (isDragHandleHovered) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                modifier = Modifier
                    .hoverable(dragHandleInteractionSource)
                    .dragAndDropSource {
                        DragAndDropTransferData(
                            transferable = DragAndDropTransferable(StringSelection("SORT:$columnKey:$sortIndex")),
                            supportedActions = listOf(DragAndDropTransferAction.Move)
                        )
                    }
            )

            // Ikona kierunku sortowania (po lewej)
            Icon(
                imageVector = when (direction) {
                    SortDirection.ASC -> Icons.Default.ArrowUpward
                    SortDirection.DESC -> Icons.Default.ArrowDownward
                },
                contentDescription = when (direction) {
                    SortDirection.ASC -> "Rosnąco"
                    SortDirection.DESC -> "Malejąco"
                },
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.clickable {
                    val newDirection = when (direction) {
                        SortDirection.ASC -> SortDirection.DESC
                        SortDirection.DESC -> SortDirection.ASC
                    }
                    onDirectionChange(newDirection)
                }
            )

            // Nazwa kolumny
            Text(
                text = columnName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            // Przycisk usuwania
            Text(
                text = "×",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.clickable { onRemove() }
            )
        }
    }
}