package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.octavius.modules.activity.timeline.TimelineEntry
import org.octavius.modules.activity.timeline.TimelineState

@Composable
fun TimelineCanvas(
    state: TimelineState,
    onSelectionChange: (Pair<Float, Float>?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var selectionStart by remember { mutableStateOf<Float?>(null) }
    var selectionEnd by remember { mutableStateOf<Float?>(null) }

    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val selectionColor = Color.Blue.copy(alpha = 0.2f)
    val defaultBlockColor = Color(0xFF6366F1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectionStart = offset.x
                        selectionEnd = offset.x
                    },
                    onDrag = { change, _ ->
                        selectionEnd = change.position.x
                        onSelectionChange(
                            selectionStart?.let { start ->
                                selectionEnd?.let { end ->
                                    minOf(start, end) to maxOf(start, end)
                                }
                            }
                        )
                    },
                    onDragEnd = {
                        // Keep selection visible
                    }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val hourWidth = state.zoomLevel

        // Draw hour grid
        for (hour in 0..24) {
            val x = (hour * hourWidth).coerceAtMost(canvasWidth)
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, canvasHeight),
                strokeWidth = 1f
            )

            // Hour labels
            if (hour < 24) {
                val label = String.format("%02d:00", hour)
                val textLayout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(x + 4, canvasHeight - 16)
                )
            }
        }

        // Draw activity blocks
        state.entries.forEach { entry ->
            val startHour = entry.startTime.hour + entry.startTime.minute / 60f
            val endHour = entry.endTime?.let { it.hour + it.minute / 60f } ?: (startHour + 0.1f)

            val x = startHour * hourWidth
            val width = (endHour - startHour) * hourWidth

            val color = entry.categoryColor?.let {
                parseColor(it)
            } ?: defaultBlockColor

            drawRect(
                color = color,
                topLeft = Offset(x, 20f),
                size = Size(width.coerceAtLeast(4f), canvasHeight - 40f)
            )
        }

        // Draw selection rectangle
        state.selectionRange?.let { (start, end) ->
            drawRect(
                color = selectionColor,
                topLeft = Offset(start, 0f),
                size = Size(end - start, canvasHeight)
            )
        }
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.removePrefix("#")
        val colorInt = when (color.length) {
            6 -> color.toLong(16).toInt() or 0xFF000000.toInt()
            8 -> color.toLong(16).toInt()
            else -> 0xFF6366F1.toInt()
        }
        Color(
            red = (colorInt shr 16 and 0xFF) / 255f,
            green = (colorInt shr 8 and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f,
            alpha = 1f
        )
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }
}
