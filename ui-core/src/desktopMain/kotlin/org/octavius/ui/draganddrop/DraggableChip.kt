package org.octavius.ui.draganddrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import java.awt.Cursor

object ChipConstants {
    val chipShape = RoundedCornerShape(16.dp)
    val horizontalPadding = 12.dp
    val verticalPadding = 6.dp
    val iconSpacing = 4.dp
    val borderWidth = 1.dp
}


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun <T: Any> DraggableChip(
    text: String,
    dragData: T,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onDrop: (T) -> Boolean = { false },
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val graphicsLayer = rememberGraphicsLayer()

    Surface(
        shape = ChipConstants.chipShape,
        color = backgroundColor,
        modifier = modifier
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
            .drawWithContent {
                graphicsLayer.record {
                    this@drawWithContent.drawContent()
                }
                drawLayer(graphicsLayer)
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = object : DragAndDropTarget {
                    override fun onDrop(event: DragAndDropEvent): Boolean {
                        val data = event.getLocalData<T>() ?: return false
                        return onDrop(data)
                    }
                }
            )
            .dragAndDropSource(
                drawDragDecoration = {
                    drawLayer(graphicsLayer)
                },
                transferData = {
                    DragAndDropTransferData(
                        transferable = DragAndDropTransferable(LocalTransferable(dragData)),
                        supportedActions = listOf(DragAndDropTransferAction.Move, DragAndDropTransferAction.Copy)
                    )
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