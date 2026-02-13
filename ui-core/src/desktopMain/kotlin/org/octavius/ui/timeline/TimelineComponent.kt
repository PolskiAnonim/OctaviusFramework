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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineComponent(
    state: TimelineState = rememberTimelineState(),
    showCurrentTime: Boolean = false,
    lanes: List<TimelineLane> = emptyList(),
    modifier: Modifier = Modifier
) {
    val currentTimeSeconds = rememberCurrentTimeSeconds(showCurrentTime)
    var localMousePos by remember { mutableStateOf<Offset?>(null) }
    var componentSize by remember { mutableStateOf(Pair(0f, 0f)) }

    val textMeasurer = rememberTextMeasurer()

    // Axis
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val axisTickColor = MaterialTheme.colorScheme.outline
    val axisLabelStyle = TextStyle(color = axisLabelColor, fontSize = 12.sp)
    val sampleLayout = textMeasurer.measure("00:00", axisLabelStyle)
    val labelWidth = sampleLayout.size.width
    val labelHeight = sampleLayout.size.height
    val tickHeight = 8f
    val axisHeight = labelHeight + tickHeight + 6f

    // Hover label
    val hoverBgColor = MaterialTheme.colorScheme.inverseSurface
    val hoverTextColor = MaterialTheme.colorScheme.inverseOnSurface
    val hoverLabelStyle = TextStyle(color = hoverTextColor, fontSize = 12.sp)

    // Colors
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val hoverLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val separatorColor = MaterialTheme.colorScheme.outline

    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { componentSize = Pair(it.width.toFloat(), it.height.toFloat()) }
            .onPointerEvent(PointerEventType.Scroll) {
                val change = it.changes.first()
                val delta = change.scrollDelta
                state.onPointerEvent(delta.x, delta.y, change.position.x)
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
            .onPointerEvent(PointerEventType.Press) {
                val pos = it.changes.first().position
                state.handleBlockClick(pos, lanes, axisHeight, componentSize.second)
            }
    ) {
        SideEffect {
            state.updateViewportWidth(constraints.maxWidth.toFloat())
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerSecond = state.pixelsPerSecond
            val scrollX = state.scrollOffset
            val interval = pickTickInterval(pxPerSecond)
            val viewportWidth = size.width

            val lanesTop = axisHeight
            val lanesHeight = size.height - axisHeight
            val laneCount = lanes.size.coerceAtLeast(1)
            val laneHeight = lanesHeight / laneCount

            translate(left = -scrollX) {
                val startSec = (scrollX / pxPerSecond).toInt().coerceAtLeast(0)
                val endSec = ((scrollX + viewportWidth) / pxPerSecond).toInt().coerceAtMost(86400)

                drawBlocks(lanes, state, lanesTop, laneHeight, pxPerSecond, startSec, endSec)

                drawGrid(interval, startSec, endSec, pxPerSecond, lineColor)

                drawAxisTicks(
                    interval, startSec, endSec, pxPerSecond,
                    axisHeight, tickHeight, axisTickColor,
                    textMeasurer, axisLabelStyle, labelWidth, labelHeight
                )

                drawTimeIndicators(
                    showCurrentTime, currentTimeSeconds, pxPerSecond,
                    state.hoverSeconds, hoverLineColor
                )
            }

            drawAxisBaseline(axisHeight, axisTickColor)
            drawLaneSeparators(laneCount, lanesTop, laneHeight, separatorColor)
            drawHoverLabel(
                state.hoverSeconds, pxPerSecond, scrollX,
                localMousePos, textMeasurer, hoverLabelStyle, hoverBgColor
            )
        }
    }
}

private fun DrawScope.drawBlocks(
    lanes: List<TimelineLane>,
    state: TimelineState,
    lanesTop: Float,
    laneHeight: Float,
    pxPerSecond: Float,
    startSec: Int,
    endSec: Int,
) {
    lanes.forEachIndexed { laneIndex, lane ->
        val laneY = lanesTop + laneIndex * laneHeight

        for (block in lane.blocks) {
            if (block.endSeconds < startSec || block.startSeconds > endSec) continue
            val blockX = block.startSeconds * pxPerSecond
            val blockW = (block.endSeconds - block.startSeconds) * pxPerSecond
            val isSelected = block == state.selectedBlock

            drawRect(
                color = block.color.copy(alpha = if (isSelected) 0.6f else 0.35f),
                topLeft = Offset(blockX, laneY),
                size = Size(blockW, laneHeight),
            )

            if (isSelected) {
                drawRect(
                    color = block.color,
                    topLeft = Offset(blockX, laneY),
                    size = Size(blockW, laneHeight),
                    style = Stroke(width = 2f),
                )
            }
        }
    }
}

private fun DrawScope.drawGrid(
    interval: Int,
    startSec: Int,
    endSec: Int,
    pxPerSecond: Float,
    lineColor: Color,
) {
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
}

private fun DrawScope.drawAxisTicks(
    interval: Int,
    startSec: Int,
    endSec: Int,
    pxPerSecond: Float,
    axisHeight: Float,
    tickHeight: Float,
    tickColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    labelWidth: Int,
    labelHeight: Int,
) {
    val firstTick = (startSec / interval) * interval
    var sec = firstTick
    while (sec <= endSec + interval) {
        if (sec in 0..86400) {
            val x = sec * pxPerSecond

            drawLine(
                color = tickColor,
                start = Offset(x, axisHeight),
                end = Offset(x, axisHeight - tickHeight),
                strokeWidth = 1f
            )

            val textLayout = textMeasurer.measure(
                text = formatTickLabel(sec),
                style = labelStyle,
                maxLines = 1,
                constraints = Constraints.fixed(labelWidth, labelHeight)
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(x - labelWidth / 2f, axisHeight - tickHeight - labelHeight - 2f)
            )
        }
        sec += interval
    }
}

private fun DrawScope.drawTimeIndicators(
    showCurrentTime: Boolean,
    currentTimeSeconds: Float,
    pxPerSecond: Float,
    hoverSeconds: Float?,
    hoverLineColor: Color,
) {
    if (showCurrentTime) {
        val nowX = currentTimeSeconds * pxPerSecond
        drawLine(
            color = Color.Red,
            start = Offset(nowX, 0f),
            end = Offset(nowX, size.height),
            strokeWidth = 2f
        )
    }

    hoverSeconds?.let { hoverSec ->
        val hoverX = hoverSec * pxPerSecond
        drawLine(
            color = hoverLineColor,
            start = Offset(hoverX, 0f),
            end = Offset(hoverX, size.height),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawAxisBaseline(axisHeight: Float, tickColor: Color) {
    drawLine(
        color = tickColor,
        start = Offset(0f, axisHeight),
        end = Offset(size.width, axisHeight),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawLaneSeparators(
    laneCount: Int,
    lanesTop: Float,
    laneHeight: Float,
    separatorColor: Color,
) {
    for (i in 1 until laneCount) {
        val y = lanesTop + i * laneHeight
        drawLine(
            color = separatorColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawHoverLabel(
    hoverSeconds: Float?,
    pxPerSecond: Float,
    scrollX: Float,
    localMousePos: Offset?,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    bgColor: Color,
) {
    hoverSeconds?.let { hoverSec ->
        val snappedToMinute = (hoverSec.toInt() / 60) * 60
        val hoverLayout = textMeasurer.measure(
            text = formatTickLabel(snappedToMinute),
            style = labelStyle,
            maxLines = 1,
        )

        val padH = 4f
        val padV = 2f
        val bgWidth = hoverLayout.size.width + padH * 2
        val bgHeight = hoverLayout.size.height + padV * 2
        val hoverViewportX = hoverSec * pxPerSecond - scrollX
        val labelLeft = (hoverViewportX - bgWidth / 2f).coerceIn(0f, size.width - bgWidth)
        val labelTop = localMousePos?.let { mouse ->
            (mouse.y + 32f).coerceAtMost(size.height - bgHeight)
        } ?: padV

        drawRoundRect(
            color = bgColor,
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
