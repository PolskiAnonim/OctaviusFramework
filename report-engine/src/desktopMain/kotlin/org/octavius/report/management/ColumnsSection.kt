package org.octavius.report.management

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import org.octavius.report.component.LocalReportHandler
import org.octavius.report.component.ReportState
import org.octavius.ui.draganddrop.ChipConstants
import org.octavius.ui.draganddrop.DraggableChip

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnsSection(
    reportState: ReportState
) {
    val reportHandler = LocalReportHandler.current
    val allColumns = reportState.columnKeysOrder
    val visibleColumns = reportState.visibleColumns

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
            items(allColumns.filter { reportHandler.reportStructure.manageableColumnKeys.contains(it) }) { columnKey ->
                val isVisible = visibleColumns.contains(columnKey)
                val index = allColumns.indexOf(columnKey)
                DraggableChip(
                    text = reportHandler.reportStructure.getColumn(columnKey).header,
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
                                reportHandler.reorderColumns(fromIndex, index)
                                return@DraggableChip true
                            }
                        }
                        false
                    },
                    leadingIcon = {
                        Box(modifier = Modifier.clickable {
                            reportHandler.toggleColumnVisibility(columnKey, isVisible)
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