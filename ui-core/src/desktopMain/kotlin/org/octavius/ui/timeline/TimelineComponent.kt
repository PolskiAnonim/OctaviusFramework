package org.octavius.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineComponent(
    state: TimelineState = rememberTimelineState(),
    showCurrentTime: Boolean = false,
    lanes: List<TimelineLane> = emptyList(),
    theme: TimelineTheme = rememberTimelineTheme(),
    contextMenuContent: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentTimeSeconds = rememberCurrentTimeSeconds(showCurrentTime)
    var localMousePos by remember { mutableStateOf<Offset?>(null) }
    var componentSize by remember { mutableStateOf(Pair(0f, 0f)) }

    var pressStartPos by remember { mutableStateOf<Offset?>(null) }
    var isRangeDragging by remember { mutableStateOf(false) }

    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuAnchorPx by remember { mutableStateOf(Offset.Zero) }
    var badgeSize by remember { mutableStateOf(IntSize.Zero) }

    val textMeasurer = rememberTextMeasurer()
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
                    if (isRangeDragging) state.onSelectionDragUpdate(pos.x)
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
                        if (!isBadgeHit(pos, state, componentSize.first, badgeSize, axisHeight)) {
                            pressStartPos = pos
                            isRangeDragging = false
                        }
                    }
                    PointerButton.Secondary -> {
                        val sel = state.selection
                        if (sel != null && pos.y >= axisHeight) {
                            val selMinVx = sel.minSeconds * state.pixelsPerSecond - state.scrollOffset
                            val selMaxVx = sel.maxSeconds * state.pixelsPerSecond - state.scrollOffset
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
        SideEffect { state.updateViewportWidth(constraints.maxWidth.toFloat()) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerSecond = state.pixelsPerSecond
            val scrollX = state.scrollOffset
            val interval = pickTickInterval(pxPerSecond)
            val lanesTop = axisHeight
            val lanesHeight = size.height - axisHeight
            val laneCount = lanes.size.coerceAtLeast(1)
            val laneHeight = lanesHeight / laneCount

            translate(left = -scrollX) {
                val startSec = (scrollX / pxPerSecond).toInt().coerceAtLeast(0)
                val endSec = ((scrollX + size.width) / pxPerSecond).toInt().coerceAtMost(86400)

                drawBlocks(lanes, state, lanesTop, laneHeight, pxPerSecond, startSec, endSec)
                drawGrid(interval, startSec, endSec, pxPerSecond, theme.grid)
                drawAxisTicks(interval, startSec, endSec, pxPerSecond, axisHeight, labelWidth, labelHeight, textMeasurer, theme.axis)
                drawTimeIndicators(showCurrentTime, currentTimeSeconds, pxPerSecond, state.hoverSeconds, theme.hover.lineColor)
                drawSelectionOverlay(state.selection, lanesTop, lanesHeight, pxPerSecond, theme.selection)
            }

            drawAxisBaseline(axisHeight, theme.axis.tickColor)
            drawLaneSeparators(laneCount, lanesTop, laneHeight, theme.laneSeparatorColor)
            drawLaneLabels(lanes, lanesTop, laneHeight, localMousePos, textMeasurer, theme.laneLabel)
            drawHoverLabel(state.hoverSeconds, state.hoveredBlock, pxPerSecond, scrollX, localMousePos, textMeasurer, theme.hover)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            TimelineSelectionOverlay(
                state = state,
                axisHeight = axisHeight,
                componentWidth = componentSize.first,
                badgeSize = badgeSize,
                onBadgeSizeChanged = { badgeSize = it },
                selectionStyle = theme.selection,
                showContextMenu = showContextMenu,
                contextMenuAnchorPx = contextMenuAnchorPx,
                onShowContextMenu = { anchor ->
                    contextMenuAnchorPx = anchor
                    showContextMenu = true
                },
                onDismissContextMenu = { showContextMenu = false },
                contextMenuContent = contextMenuContent,
            )
        }
    }
}

/** Returns true when [pos] lands within the selection badge bounds. */
private fun isBadgeHit(
    pos: Offset,
    state: TimelineState,
    componentWidth: Float,
    badgeSize: IntSize,
    axisHeight: Float,
): Boolean {
    val sel = state.selection ?: return false
    if (badgeSize == IntSize.Zero) return false
    val selMinVx = (sel.minSeconds * state.pixelsPerSecond - state.scrollOffset).coerceIn(0f, componentWidth)
    val selMaxVx = (sel.maxSeconds * state.pixelsPerSecond - state.scrollOffset).coerceIn(0f, componentWidth)
    val cx = (selMinVx + selMaxVx) / 2f
    val left = (cx - badgeSize.width / 2f).coerceIn(0f, (componentWidth - badgeSize.width).coerceAtLeast(0f))
    val top = axisHeight + 4f
    return pos.x in left..(left + badgeSize.width) && pos.y in top..(top + badgeSize.height)
}
