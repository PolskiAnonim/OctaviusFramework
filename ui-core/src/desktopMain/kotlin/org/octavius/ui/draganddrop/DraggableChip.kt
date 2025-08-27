package org.octavius.ui.draganddrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import java.awt.datatransfer.StringSelection

object ChipConstants {
    val chipShape = RoundedCornerShape(16.dp)
    val horizontalPadding = 12.dp
    val verticalPadding = 6.dp
    val iconSpacing = 4.dp
    val borderWidth = 1.dp
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DraggableChip(
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