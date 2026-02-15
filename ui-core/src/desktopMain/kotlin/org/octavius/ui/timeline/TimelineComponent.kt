package org.octavius.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineComponent(
    state: TimelineState = rememberTimelineState(),
    showCurrentTime: Boolean = false,
    lanes: List<TimelineLane> = emptyList(),
    theme: TimelineTheme = rememberTimelineTheme(),
    modifier: Modifier = Modifier
) {
    val currentTimeSeconds = rememberCurrentTimeSeconds(showCurrentTime)
    var localMousePos by remember { mutableStateOf<Offset?>(null) }
    var componentSize by remember { mutableStateOf(Pair(0f, 0f)) }

    val textMeasurer = rememberTextMeasurer()

    // Axis measurements (derived from theme text style)
    val sampleLayout = textMeasurer.measure("00:00", theme.axis.textStyle)
    val labelWidth = sampleLayout.size.width
    val labelHeight = sampleLayout.size.height
    val axisHeight = labelHeight + theme.axis.tickHeight + 6f

    BoxWithConstraints(
        modifier = modifier
            .background(theme.backgroundColor)
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
                state.onHoverMove(pos, lanes, axisHeight, componentSize.second)
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
                drawGrid(interval, startSec, endSec, pxPerSecond, theme.grid)
                drawAxisTicks(
                    interval, startSec, endSec, pxPerSecond,
                    axisHeight, labelWidth, labelHeight,
                    textMeasurer, theme.axis
                )
                drawTimeIndicators(
                    showCurrentTime, currentTimeSeconds, pxPerSecond,
                    state.hoverSeconds, theme.hover.lineColor
                )
            }

            drawAxisBaseline(axisHeight, theme.axis.tickColor)
            drawLaneSeparators(laneCount, lanesTop, laneHeight, theme.laneSeparatorColor)
            drawLaneLabels(lanes, lanesTop, laneHeight, localMousePos, textMeasurer, theme.laneLabel)
            drawHoverLabel(
                state.hoverSeconds, state.hoveredBlock, pxPerSecond, scrollX,
                localMousePos, textMeasurer, theme.hover
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
            val isHovered = block == state.hoveredBlock

            val alpha = when {
                isSelected -> 0.6f
                isHovered -> 0.5f
                else -> 0.35f
            }

            drawRect(
                color = block.color.copy(alpha = alpha),
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
    style: TimelineTheme.GridStyle,
) {
    val firstTick = (startSec / interval) * interval
    var sec = firstTick
    while (sec <= endSec + interval) {
        if (sec in 0..86400) {
            val x = sec * pxPerSecond
            drawLine(
                color = style.lineColor,
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
    labelWidth: Int,
    labelHeight: Int,
    textMeasurer: TextMeasurer,
    style: TimelineTheme.AxisStyle,
) {
    val firstTick = (startSec / interval) * interval
    var sec = firstTick
    while (sec <= endSec + interval) {
        if (sec in 0..86400) {
            val x = sec * pxPerSecond

            drawLine(
                color = style.tickColor,
                start = Offset(x, axisHeight),
                end = Offset(x, axisHeight - style.tickHeight),
                strokeWidth = 1f
            )

            val textLayout = textMeasurer.measure(
                text = formatTickLabel(sec),
                style = style.textStyle,
                maxLines = 1,
                constraints = Constraints.fixed(labelWidth, labelHeight)
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(x - labelWidth / 2f, axisHeight - style.tickHeight - labelHeight - 2f)
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

private fun DrawScope.drawLaneLabels(
    lanes: List<TimelineLane>,
    lanesTop: Float,
    laneHeight: Float,
    localMousePos: Offset?,
    textMeasurer: TextMeasurer,
    style: TimelineTheme.LaneLabelStyle,
) {
    val padH = 8f
    val padV = 4f

    lanes.forEachIndexed { index, lane ->
        if (lane.label.isBlank()) return@forEachIndexed

        val layout = textMeasurer.measure(lane.label, style.textStyle, maxLines = 1)
        val bgWidth = layout.size.width + padH * 2
        val bgHeight = layout.size.height + padV * 2
        val laneY = lanesTop + index * laneHeight
        val labelX = padH
        val labelY = laneY + (laneHeight - bgHeight) / 2f

        val mouseOver = localMousePos?.let { mouse ->
            mouse.x in labelX..(labelX + bgWidth) && mouse.y in labelY..(labelY + bgHeight)
        } == true

        if (!mouseOver) {
            drawRoundRect(
                color = style.bgColor,
                topLeft = Offset(labelX, labelY),
                size = Size(bgWidth, bgHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawText(
                textLayoutResult = layout,
                color = style.textColor,
                topLeft = Offset(labelX + padH, labelY + padV)
            )
        }
    }
}

private fun DrawScope.drawHoverLabel(
    hoverSeconds: Float?,
    hoveredBlock: TimelineBlock?,
    pxPerSecond: Float,
    scrollX: Float,
    localMousePos: Offset?,
    textMeasurer: TextMeasurer,
    style: TimelineTheme.HoverStyle,
) {
    hoverSeconds ?: return

    val padH = 6f
    val padV = 4f
    val lineSpacing = 2f

    val snappedToMinute = (hoverSeconds.toInt() / 60) * 60
    val timeLayout = textMeasurer.measure(
        text = formatTickLabel(snappedToMinute),
        style = style.labelTextStyle,
        maxLines = 1,
    )

    val extraLines = mutableListOf<TextLayoutResult>()

    if (hoveredBlock != null) {
        val startMin = (hoveredBlock.startSeconds / 60).toInt()
        val endMin = (hoveredBlock.endSeconds / 60).toInt()
        val durationMin = endMin - startMin
        val durationH = durationMin / 60
        val durationM = durationMin % 60
        val durationStr = if (durationH > 0) "${durationH}h ${durationM}min" else "${durationM}min"
        val rangeText = "${formatTickLabel(startMin * 60)} – ${formatTickLabel(endMin * 60)} ($durationStr)"
        extraLines += textMeasurer.measure(rangeText, style.tooltipDescriptionStyle, maxLines = 1)

        if (hoveredBlock.label.isNotBlank()) {
            extraLines += textMeasurer.measure(hoveredBlock.label, style.tooltipTitleStyle, maxLines = 1)
        }
        if (hoveredBlock.description.isNotBlank()) {
            extraLines += textMeasurer.measure(hoveredBlock.description, style.tooltipDescriptionStyle, maxLines = 2)
        }
    }

    val allLayouts = listOf(timeLayout) + extraLines
    val contentWidth = allLayouts.maxOf { it.size.width }
    val contentHeight = allLayouts.sumOf { it.size.height } + (lineSpacing * (allLayouts.size - 1)).toInt()

    val bgWidth = contentWidth + padH * 2
    val bgHeight = contentHeight + padV * 2
    val hoverViewportX = hoverSeconds * pxPerSecond - scrollX
    val labelLeft = (hoverViewportX - bgWidth / 2f).coerceIn(0f, size.width - bgWidth)
    val labelTop = localMousePos?.let { mouse ->
        (mouse.y + 32f).coerceAtMost(size.height - bgHeight)
    } ?: padV

    drawRoundRect(
        color = style.labelBgColor,
        topLeft = Offset(labelLeft, labelTop),
        size = Size(bgWidth, bgHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )

    var yOffset = labelTop + padV
    for (layout in allLayouts) {
        drawText(textLayoutResult = layout, topLeft = Offset(labelLeft + padH, yOffset))
        yOffset += layout.size.height + lineSpacing
    }
}
