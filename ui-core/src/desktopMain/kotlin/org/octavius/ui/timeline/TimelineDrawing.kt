package org.octavius.ui.timeline

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints

internal fun DrawScope.drawBlocks(
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

internal fun DrawScope.drawGrid(
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
            drawLine(color = style.lineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        }
        sec += interval
    }
}

internal fun DrawScope.drawAxisTicks(
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
                strokeWidth = 1f,
            )
            val textLayout = textMeasurer.measure(
                text = formatTickLabel(sec),
                style = style.textStyle,
                maxLines = 1,
                constraints = Constraints.fixed(labelWidth, labelHeight),
            )
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(x - labelWidth / 2f, axisHeight - style.tickHeight - labelHeight - 2f),
            )
        }
        sec += interval
    }
}

internal fun DrawScope.drawTimeIndicators(
    showCurrentTime: Boolean,
    currentTimeSeconds: Float,
    pxPerSecond: Float,
    hoverSeconds: Float?,
    hoverLineColor: Color,
) {
    if (showCurrentTime) {
        val nowX = currentTimeSeconds * pxPerSecond
        drawLine(color = Color.Red, start = Offset(nowX, 0f), end = Offset(nowX, size.height), strokeWidth = 2f)
    }
    hoverSeconds?.let { sec ->
        val x = sec * pxPerSecond
        drawLine(color = hoverLineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
    }
}

internal fun DrawScope.drawSelectionOverlay(
    selection: TimeSelection?,
    lanesTop: Float,
    lanesHeight: Float,
    pxPerSecond: Float,
    style: TimelineTheme.SelectionStyle,
) {
    selection ?: return
    val startX = selection.minSeconds * pxPerSecond
    val endX = selection.maxSeconds * pxPerSecond
    drawRect(
        color = style.overlayColor,
        topLeft = Offset(startX, lanesTop),
        size = Size(endX - startX, lanesHeight),
    )
    drawLine(color = style.borderColor, start = Offset(startX, lanesTop), end = Offset(startX, lanesTop + lanesHeight), strokeWidth = 1.5f)
    drawLine(color = style.borderColor, start = Offset(endX, lanesTop), end = Offset(endX, lanesTop + lanesHeight), strokeWidth = 1.5f)
}

internal fun DrawScope.drawAxisBaseline(axisHeight: Float, tickColor: Color) {
    drawLine(color = tickColor, start = Offset(0f, axisHeight), end = Offset(size.width, axisHeight), strokeWidth = 1f)
}

internal fun DrawScope.drawLaneSeparators(
    laneCount: Int,
    lanesTop: Float,
    laneHeight: Float,
    separatorColor: Color,
) {
    for (i in 1 until laneCount) {
        val y = lanesTop + i * laneHeight
        drawLine(color = separatorColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
    }
}

internal fun DrawScope.drawLaneLabels(
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
            drawRoundRect(color = style.bgColor, topLeft = Offset(labelX, labelY), size = Size(bgWidth, bgHeight), cornerRadius = CornerRadius(4f, 4f))
            drawText(textLayoutResult = layout, color = style.textColor, topLeft = Offset(labelX + padH, labelY + padV))
        }
    }
}

internal fun DrawScope.drawHoverLabel(
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
    val timeLayout = textMeasurer.measure(text = formatTickLabel(snappedToMinute), style = style.labelTextStyle, maxLines = 1)
    val extraLines = mutableListOf<TextLayoutResult>()

    if (hoveredBlock != null) {
        val startMin = (hoveredBlock.startSeconds / 60).toInt()
        val endMin = (hoveredBlock.endSeconds / 60).toInt()
        val durationMin = endMin - startMin
        val durationH = durationMin / 60
        val durationM = durationMin % 60
        val durationStr = if (durationH > 0) "${durationH}h ${durationM}min" else "${durationM}min"
        extraLines += textMeasurer.measure("${formatTickLabel(startMin * 60)} – ${formatTickLabel(endMin * 60)} ($durationStr)", style.tooltipDescriptionStyle, maxLines = 1)
        if (hoveredBlock.label.isNotBlank()) extraLines += textMeasurer.measure(hoveredBlock.label, style.tooltipTitleStyle, maxLines = 1)
        if (hoveredBlock.description.isNotBlank()) extraLines += textMeasurer.measure(hoveredBlock.description, style.tooltipDescriptionStyle, maxLines = 2)
    }

    val allLayouts = listOf(timeLayout) + extraLines
    val contentWidth = allLayouts.maxOf { it.size.width }
    val contentHeight = allLayouts.sumOf { it.size.height } + (lineSpacing * (allLayouts.size - 1)).toInt()
    val bgWidth = contentWidth + padH * 2
    val bgHeight = contentHeight + padV * 2
    val hoverViewportX = hoverSeconds * pxPerSecond - scrollX
    val labelLeft = (hoverViewportX - bgWidth / 2f).coerceIn(0f, (size.width - bgWidth).coerceAtLeast(0f))
    val labelTop = localMousePos?.let { mouse -> (mouse.y + 32f).coerceAtMost((size.height - bgHeight).coerceAtLeast(0f)) } ?: padV

    drawRoundRect(color = style.labelBgColor, topLeft = Offset(labelLeft, labelTop), size = Size(bgWidth, bgHeight), cornerRadius = CornerRadius(4f, 4f))
    var yOffset = labelTop + padV
    for (layout in allLayouts) {
        drawText(textLayoutResult = layout, topLeft = Offset(labelLeft + padH, yOffset))
        yOffset += layout.size.height + lineSpacing
    }
}
