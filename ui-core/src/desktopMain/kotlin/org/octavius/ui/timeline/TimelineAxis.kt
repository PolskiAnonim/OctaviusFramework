package org.octavius.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineAxis(
    state: TimelineState = rememberTimelineState(),
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.LightGray, fontSize = 12.sp)

    // Pre-measure a sample label to know the fixed width
    val sampleLayout = textMeasurer.measure("00:00", labelStyle)
    val labelWidth = sampleLayout.size.width
    val labelHeight = sampleLayout.size.height

    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .onPointerEvent(PointerEventType.Scroll) {
                val change = it.changes.first()
                val delta = change.scrollDelta
                val mouseX = change.position.x

                state.onPointerEvent(delta.x, delta.y, mouseX)
                change.consume()
            }
    ) {
        val width = constraints.maxWidth.toFloat()

        SideEffect {
            state.updateViewportWidth(width)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerMinute = state.pixelsPerMinute
            val scrollX = state.scrollOffset
            val tickHeight = 8f

            val bottomY = size.height

            translate(left = -scrollX) {
                val startMinute = (scrollX / pxPerMinute).toInt().coerceAtLeast(0)
                val endMinute = ((scrollX + width) / pxPerMinute).toInt().coerceAtMost(1440)

                for (h in 0..24) {
                    val min = h * 60
                    if (min in (startMinute - 60)..(endMinute + 60)) {
                        val x = min * pxPerMinute

                        // Tick od dołu w górę
                        drawLine(
                            color = Color.Gray,
                            start = Offset(x, bottomY),
                            end = Offset(x, bottomY - tickHeight),
                            strokeWidth = 1f
                        )

                        val textLayout = textMeasurer.measure(
                            text = "%02d:00".format(h),
                            style = labelStyle,
                            maxLines = 1,
                            constraints = androidx.compose.ui.unit.Constraints.fixed(labelWidth, labelHeight)
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(x - labelWidth / 2f, bottomY - tickHeight - labelHeight - 2f)
                        )
                    }
                }
            }

            // Linia bazowa na dole
            drawLine(
                color = Color.Gray,
                start = Offset(0f, bottomY),
                end = Offset(size.width, bottomY),
                strokeWidth = 1f
            )
        }
    }
}
