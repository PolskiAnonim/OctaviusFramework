package org.octavius.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
fun TimelineComponent(
    state: TimelineState = rememberTimelineState(),
    showCurrentTime: Boolean = false,
    blocks: List<TimelineBlock> = emptyList(),
    modifier: Modifier = Modifier
) {
    val currentTimeSeconds = rememberCurrentTimeSeconds(showCurrentTime)
    var localMousePos by remember { mutableStateOf<Offset?>(null) }

    val textMeasurer = rememberTextMeasurer()
    val hoverBgColor = MaterialTheme.colorScheme.inverseSurface
    val hoverTextColor = MaterialTheme.colorScheme.inverseOnSurface
    val hoverLabelStyle = TextStyle(color = hoverTextColor, fontSize = 12.sp)

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
                val pos = it.changes.first().position
                localMousePos = pos
                state.onHoverMove(pos.x)
            }
            .onPointerEvent(PointerEventType.Exit) {
                localMousePos = null
                state.onHoverExit()
            }
    ) {
        val width = constraints.maxWidth.toFloat()

        SideEffect {
            state.updateViewportWidth(width)
        }

        val lineColor = MaterialTheme.colorScheme.outlineVariant
        val hoverLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerSecond = state.pixelsPerSecond
            val scrollX = state.scrollOffset
            val interval = pickTickInterval(pxPerSecond)

            translate(left = -scrollX) {
                val startSec = (scrollX / pxPerSecond).toInt().coerceAtLeast(0)
                val endSec = ((scrollX + width) / pxPerSecond).toInt().coerceAtMost(86400)

                for (block in blocks) {
                    if (block.endSeconds < startSec || block.startSeconds > endSec) continue
                    val blockX = block.startSeconds * pxPerSecond
                    val blockW = (block.endSeconds - block.startSeconds) * pxPerSecond
                    drawRect(
                        color = block.color.copy(alpha = 0.4f),
                        topLeft = Offset(blockX, 0f),
                        size = Size(blockW, size.height),
                    )
                }

                val firstTick = (startSec / interval) * interval
                var sec = firstTick
                while (sec <= endSec + interval) {
                    if (sec in 0..86400) {
                        val x = sec * pxPerSecond

                        drawLine(
                            color = lineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }
                    sec += interval
                }

                if (showCurrentTime) {
                    val nowX = currentTimeSeconds * pxPerSecond
                    drawLine(
                        color = Color.Red,
                        start = Offset(nowX, 0f),
                        end = Offset(nowX, size.height),
                        strokeWidth = 2f
                    )
                }

                state.hoverSeconds?.let { hoverSec ->
                    val hoverX = hoverSec * pxPerSecond
                    drawLine(
                        color = hoverLineColor,
                        start = Offset(hoverX, 0f),
                        end = Offset(hoverX, size.height),
                        strokeWidth = 1f
                    )
                }
            }

            // Labelka przy linii hover (viewport coordinates)
            state.hoverSeconds?.let { hoverSec ->
                val snappedToMinute = (hoverSec.toInt() / 60) * 60
                val hoverLayout = textMeasurer.measure(
                    text = formatTickLabel(snappedToMinute),
                    style = hoverLabelStyle,
                    maxLines = 1,
                )

                val padH = 4f
                val padV = 2f
                val bgWidth = hoverLayout.size.width + padH * 2
                val bgHeight = hoverLayout.size.height + padV * 2
                // Pozycja X: wycentrowana na linii hover
                val hoverViewportX = hoverSec * pxPerSecond - scrollX
                val labelLeft = (hoverViewportX - bgWidth / 2f).coerceIn(0f, size.width - bgWidth)
                // Pozycja Y: pod kursorem gdy nad tym komponentem, inaczej u góry
                val labelTop = localMousePos?.let { mouse ->
                    (mouse.y + 32f).coerceAtMost(size.height - bgHeight)
                } ?: padV

                drawRoundRect(
                    color = hoverBgColor,
                    topLeft = Offset(labelLeft, labelTop),
                    size = Size(bgWidth, bgHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawText(
                    textLayoutResult = hoverLayout,
                    topLeft = Offset(labelLeft + padH, labelTop + padV)
                )
            }
        }
    }
}
