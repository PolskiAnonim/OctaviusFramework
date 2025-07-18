package org.octavius.report.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.domain.SortDirection
import org.octavius.localization.Translations
import org.octavius.report.component.LocalReportHandler
import org.octavius.report.component.ReportState
import org.octavius.ui.component.DraggableChip
import org.octavius.ui.component.DropZone
import org.octavius.ui.component.DropZoneConstants

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SortingSection(
    reportState: ReportState
) {
    val reportHandler = LocalReportHandler.current

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
                        text = reportHandler.reportStructure.getColumn(columnKey).header,
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
                                        reportHandler.reorderSortColumns(fromIndex, sortIndex)
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
                                    reportHandler.updateSortDirection(
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
                                modifier = Modifier.clickable {
                                    reportHandler.removeSortColumn(columnKey)
                                }
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
            onDrop = { transferData ->
                val columnKey = if (transferData.contains(":")) {
                    transferData.split(":")[0]
                } else {
                    transferData
                }

                return@DropZone reportHandler.addSortColumn(columnKey)
            }
        ) { isHovered ->
            val textKey = if (reportState.sortOrder.isEmpty()) {
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