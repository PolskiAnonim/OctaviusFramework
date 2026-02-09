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
    showCurrentTime: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentTimeSeconds = rememberCurrentTimeSeconds(showCurrentTime)

    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tickColor = MaterialTheme.colorScheme.outline
    val labelStyle = TextStyle(color = labelColor, fontSize = 12.sp)

    val sampleLayout = textMeasurer.measure("00:00", labelStyle)
    val labelWidth = sampleLayout.size.width
    val labelHeight = sampleLayout.size.height

    val hoverLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

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
            .onPointerEvent(PointerEventType.Move) {
                state.onHoverMove(it.changes.first().position.x)
            }
            .onPointerEvent(PointerEventType.Exit) {
                state.onHoverExit()
            }
    ) {
        val width = constraints.maxWidth.toFloat()

        SideEffect {
            state.updateViewportWidth(width)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerSecond = state.pixelsPerSecond
            val scrollX = state.scrollOffset
            val tickHeight = 8f
            val bottomY = size.height
            val interval = pickTickInterval(pxPerSecond)

            translate(left = -scrollX) {
                val startSec = (scrollX / pxPerSecond).toInt().coerceAtLeast(0)
                val endSec = ((scrollX + width) / pxPerSecond).toInt().coerceAtMost(86400)

                val firstTick = (startSec / interval) * interval
                var sec = firstTick
                while (sec <= endSec + interval) {
                    if (sec in 0..86400) {
                        val x = sec * pxPerSecond

                        drawLine(
                            color = tickColor,
                            start = Offset(x, bottomY),
                            end = Offset(x, bottomY - tickHeight),
                            strokeWidth = 1f
                        )

                        val textLayout = textMeasurer.measure(
                            text = formatTickLabel(sec),
                            style = labelStyle,
                            maxLines = 1,
                            constraints = androidx.compose.ui.unit.Constraints.fixed(labelWidth, labelHeight)
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(x - labelWidth / 2f, bottomY - tickHeight - labelHeight - 2f)
                        )
                    }
                    sec += interval
                }

                if (showCurrentTime) {
                    val nowX = currentTimeSeconds * pxPerSecond
                    drawLine(
                        color = Color.Red,
                        start = Offset(nowX, 0f),
                        end = Offset(nowX, bottomY),
                        strokeWidth = 2f
                    )
                }

                state.hoverSeconds?.let { hoverSec ->
                    val hoverX = hoverSec * pxPerSecond
                    drawLine(
                        color = hoverLineColor,
                        start = Offset(hoverX, 0f),
                        end = Offset(hoverX, bottomY),
                        strokeWidth = 1f
                    )
                }
            }

            // Linia bazowa na dole
            drawLine(
                color = tickColor,
                start = Offset(0f, bottomY),
                end = Offset(size.width, bottomY),
                strokeWidth = 1f
            )
        }
    }
}
