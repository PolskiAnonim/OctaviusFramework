package org.octavius.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

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

    // Drag-selection state
    var pressStartPos by remember { mutableStateOf<Offset?>(null) }
    var isRangeDragging by remember { mutableStateOf(false) }

    // Context menu state
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuAnchorPx by remember { mutableStateOf(Offset.Zero) }
    var badgeSize by remember { mutableStateOf(IntSize.Zero) }

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

                val start = pressStartPos
                if (start != null) {
                    if (!isRangeDragging && abs(pos.x - start.x) > 4f) {
                        isRangeDragging = true
                        state.onSelectionDragStart(start.x, start.y, axisHeight)
                    }
                    if (isRangeDragging) {
                        state.onSelectionDragUpdate(pos.x)
                    }
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                localMousePos = null
                state.onHoverExit()
            }
            .onPointerEvent(PointerEventType.Press) {
                val pos = it.changes.first().position
                when (it.button) {
                    PointerButton.Primary -> {
                        // Ignore presses on the selection badge so the arrow button
                        // can handle its own click without the Release handler stomping on it.
                        val onBadge = run {
                            val sel = state.selection
                            if (sel == null || badgeSize == IntSize.Zero) return@run false
                            val pxs = state.pixelsPerSecond
                            val sc = state.scrollOffset
                            val vw = componentSize.first
                            val selMinVx = (sel.minSeconds * pxs - sc).coerceIn(0f, vw)
                            val selMaxVx = (sel.maxSeconds * pxs - sc).coerceIn(0f, vw)
                            val cx = (selMinVx + selMaxVx) / 2f
                            val bLeft = (cx - badgeSize.width / 2f).coerceIn(0f, vw - badgeSize.width)
                            val bTop = axisHeight + 4f
                            pos.x in bLeft..(bLeft + badgeSize.width) && pos.y in bTop..(bTop + badgeSize.height)
                        }
                        if (!onBadge) {
                            pressStartPos = pos
                            isRangeDragging = false
                        }
                    }
                    PointerButton.Secondary -> {
                        val sel = state.selection
                        if (sel != null && pos.y >= axisHeight) {
                            val pxs = state.pixelsPerSecond
                            val sc = state.scrollOffset
                            val selMinVx = sel.minSeconds * pxs - sc
                            val selMaxVx = sel.maxSeconds * pxs - sc
                            if (pos.x in selMinVx..selMaxVx) {
                                contextMenuAnchorPx = pos
                                showContextMenu = true
                            }
                        }
                    }
                    else -> {}
                }
            }
            .onPointerEvent(PointerEventType.Release) {
                val pos = it.changes.first().position
                if (isRangeDragging) {
                    state.onSelectionDragEnd()
                    isRangeDragging = false
                } else if (pressStartPos != null && it.button == PointerButton.Primary) {
                    state.handleBlockClick(pos, lanes, axisHeight, componentSize.second)
                }
                pressStartPos = null
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
                drawSelectionOverlay(state.selection, lanesTop, lanesHeight, pxPerSecond, theme.selection)
            }

            drawAxisBaseline(axisHeight, theme.axis.tickColor)
            drawLaneSeparators(laneCount, lanesTop, laneHeight, theme.laneSeparatorColor)
            drawLaneLabels(lanes, lanesTop, laneHeight, localMousePos, textMeasurer, theme.laneLabel)
            drawHoverLabel(
                state.hoverSeconds, state.hoveredBlock, pxPerSecond, scrollX,
                localMousePos, textMeasurer, theme.hover
            )
        }

        // Compose overlay: selection badge and context menu
        Box(modifier = Modifier.fillMaxSize()) {
            val sel = state.selection
            if (sel != null) {
                val pxs = state.pixelsPerSecond
                val sc = state.scrollOffset
                val vw = componentSize.first

                val selMinVx = (sel.minSeconds * pxs - sc).coerceIn(0f, vw)
                val selMaxVx = (sel.maxSeconds * pxs - sc).coerceIn(0f, vw)
                val visibleCenterX = (selMinVx + selMaxVx) / 2f
                val badgeTopPx = axisHeight + 4f

                Box(
                    modifier = Modifier
                        .absoluteOffset {
                            val offsetX = (visibleCenterX - badgeSize.width / 2f)
                                .coerceIn(0f, (vw - badgeSize.width).coerceAtLeast(0f))
                            IntOffset(offsetX.roundToInt(), badgeTopPx.roundToInt())
                        }
                        .onSizeChanged { badgeSize = it }
                ) {
                    SelectionBadge(
                        selection = sel,
                        theme = theme.selection,
                        onArrowClick = {
                            contextMenuAnchorPx = Offset(
                                (visibleCenterX - badgeSize.width / 2f)
                                    .coerceIn(0f, vw - badgeSize.width),
                                badgeTopPx + badgeSize.height
                            )
                            showContextMenu = true
                        }
                    )
                }
            }

            // Context menu anchor — positioned at right-click or badge arrow
            Box(
                modifier = Modifier.absoluteOffset {
                    IntOffset(
                        contextMenuAnchorPx.x.roundToInt(),
                        contextMenuAnchorPx.y.roundToInt()
                    )
                }
            ) {
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                ) {
                    val sel = state.selection
                    if (sel != null) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${formatTimeSeconds(sel.minSeconds)} – ${formatTimeSeconds(sel.maxSeconds)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = formatDuration(sel.durationSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBadge(
    selection: TimeSelection,
    theme: TimelineTheme.SelectionStyle,
    onArrowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.badgeBgColor)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = formatDuration(selection.durationSeconds),
            style = theme.badgeTextStyle,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onArrowClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "▾",
                style = theme.badgeTextStyle.copy(color = theme.badgeIconColor),
            )
        }
    }
}

private fun DrawScope.drawSelectionOverlay(
    selection: TimeSelection?,
    lanesTop: Float,
    lanesHeight: Float,
    pxPerSecond: Float,
    style: TimelineTheme.SelectionStyle,
) {
    selection ?: return

    val startX = selection.minSeconds * pxPerSecond
    val endX = selection.maxSeconds * pxPerSecond
    val width = endX - startX

    drawRect(
        color = style.overlayColor,
        topLeft = Offset(startX, lanesTop),
        size = Size(width, lanesHeight),
    )

    drawLine(
        color = style.borderColor,
        start = Offset(startX, lanesTop),
        end = Offset(startX, lanesTop + lanesHeight),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = style.borderColor,
        start = Offset(endX, lanesTop),
        end = Offset(endX, lanesTop + lanesHeight),
        strokeWidth = 1.5f,
    )
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
