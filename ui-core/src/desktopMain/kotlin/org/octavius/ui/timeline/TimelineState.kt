package org.octavius.ui.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.max

data class TimelineBlock(
    val startSeconds: Float,
    val endSeconds: Float,
    val color: Color,
    val label: String = "",
    val description: String = "",
)

data class TimelineLane(
    val label: String,
    val blocks: List<TimelineBlock>,
)

data class TimeSelection(
    val startSeconds: Float,
    val endSeconds: Float,
) {
    val minSeconds: Float get() = minOf(startSeconds, endSeconds)
    val maxSeconds: Float get() = maxOf(startSeconds, endSeconds)
    val durationSeconds: Float get() = maxSeconds - minSeconds
}

class TimelineState {
    val totalSeconds = 86400 // 24h * 60min * 60s

    var pixelsPerSecond by mutableStateOf(0f)
        private set

    var scrollOffset by mutableStateOf(0f)
        private set

    /** Czas w sekundach pod kursorem, null gdy kursor poza komponentem */
    var hoverSeconds by mutableStateOf<Float?>(null)

    /** Aktualnie zaznaczony bloczek, null gdy nic nie zaznaczone */
    var selectedBlock by mutableStateOf<TimelineBlock?>(null)

    /** Indeks lane'a zaznaczonego bloczku, null gdy nic nie zaznaczone */
    var selectedBlockLaneIndex by mutableStateOf<Int?>(null)
        private set

    /** Bloczek pod kursorem, null gdy kursor nie jest na żadnym bloku */
    var hoveredBlock by mutableStateOf<TimelineBlock?>(null)

    /** Zaznaczony przedział czasowy, null gdy nic nie zaznaczone */
    var selection by mutableStateOf<TimeSelection?>(null)

    /** Czy trwa przeciąganie zaznaczenia */
    var isDragging by mutableStateOf(false)

    private var viewportWidth = 0f
    private var dragStartSeconds: Float? = null

    fun updateViewportWidth(width: Float) {
        if (width == viewportWidth) return
        viewportWidth = width
        enforceConstraints()
    }

    fun onHoverMove(pos: Offset, lanes: List<TimelineLane>, axisHeight: Float, componentHeight: Float) {
        if (pixelsPerSecond <= 0f) return
        val sec = (scrollOffset + pos.x) / pixelsPerSecond
        hoverSeconds = sec.coerceIn(0f, totalSeconds.toFloat())
        hoveredBlock = hitTestBlock(pos, lanes, axisHeight, componentHeight)
    }

    fun onHoverExit() {
        hoverSeconds = null
        hoveredBlock = null
    }

    fun onPointerEvent(deltaX: Float, deltaY: Float, mouseX: Float) {
        if (deltaY != 0f && deltaX == 0f) {
            val zoomFactor = if (deltaY > 0) 0.9f else 1.1f
            applyZoom(zoomFactor, mouseX)
        } else {
            val moveDelta = if (deltaX != 0f) deltaX else deltaY
            applyScroll(moveDelta)
        }
    }

    private fun applyZoom(factor: Float, pivotX: Float) {
        val oldPx = pixelsPerSecond
        val timeAtMouse = (scrollOffset + pivotX) / oldPx

        var newPx = oldPx * factor

        if (viewportWidth > 0) {
            val minPx = viewportWidth / totalSeconds
            val maxPx = 2f // max ~120px na minutę
            newPx = newPx.coerceIn(minPx, maxPx)
        }
        pixelsPerSecond = newPx

        scrollOffset = (timeAtMouse * newPx) - pivotX
        enforceScrollLimits()
    }

    private fun applyScroll(delta: Float) {
        val scrollSpeed = 25f
        scrollOffset += (delta * scrollSpeed)
        enforceScrollLimits()
    }

    private fun enforceConstraints() {
        if (viewportWidth <= 0) return
        val minPx = viewportWidth / totalSeconds
        if (pixelsPerSecond < minPx) pixelsPerSecond = minPx
        enforceScrollLimits()
    }

    fun handleBlockClick(pos: Offset, lanes: List<TimelineLane>, axisHeight: Float, componentHeight: Float) {
        selection = null
        val hitBlock = hitTestBlock(pos, lanes, axisHeight, componentHeight)
        selectedBlock = if (hitBlock == selectedBlock) null else hitBlock
        selectedBlockLaneIndex = if (selectedBlock != null) {
            val lanesHeight = componentHeight - axisHeight
            val laneHeight = lanesHeight / lanes.size.coerceAtLeast(1)
            ((pos.y - axisHeight) / laneHeight).toInt().coerceIn(0, lanes.lastIndex)
        } else null
    }

    fun selectBlock(block: TimelineBlock, laneIndex: Int) {
        selection = null
        selectedBlock = block
        selectedBlockLaneIndex = laneIndex
    }

    internal fun hitTestBlockWithLane(pos: Offset, lanes: List<TimelineLane>, axisHeight: Float, componentHeight: Float): Pair<TimelineBlock, Int>? {
        if (lanes.isEmpty() || componentHeight <= axisHeight || pixelsPerSecond <= 0f) return null
        if (pos.y < axisHeight) return null
        val lanesHeight = componentHeight - axisHeight
        val laneHeight = lanesHeight / lanes.size
        val laneIndex = ((pos.y - axisHeight) / laneHeight).toInt().coerceIn(0, lanes.lastIndex)
        val seconds = (scrollOffset + pos.x) / pixelsPerSecond
        val block = lanes[laneIndex].blocks.firstOrNull { block ->
            seconds in block.startSeconds..block.endSeconds
        }
        return block?.let { it to laneIndex }
    }

    fun onSelectionDragStart(posX: Float, posY: Float, axisHeight: Float) {
        if (pixelsPerSecond <= 0f || posY < axisHeight) {
            dragStartSeconds = null
            return
        }
        dragStartSeconds = ((scrollOffset + posX) / pixelsPerSecond).coerceIn(0f, totalSeconds.toFloat())
        isDragging = true
        selection = null
        selectedBlock = null
        selectedBlockLaneIndex = null
    }

    fun onSelectionDragUpdate(posX: Float) {
        val start = dragStartSeconds ?: return
        if (pixelsPerSecond <= 0f) return
        val end = ((scrollOffset + posX) / pixelsPerSecond).coerceIn(0f, totalSeconds.toFloat())
        selection = TimeSelection(start, end)
    }

    fun onSelectionDragEnd() {
        isDragging = false
        dragStartSeconds = null
        if ((selection?.durationSeconds ?: 0f) < 1f) selection = null
    }

    fun clearSelection() {
        selection = null
        isDragging = false
        dragStartSeconds = null
    }

    private fun hitTestBlock(pos: Offset, lanes: List<TimelineLane>, axisHeight: Float, componentHeight: Float): TimelineBlock? {
        if (lanes.isEmpty() || componentHeight <= axisHeight || pixelsPerSecond <= 0f) return null
        if (pos.y < axisHeight) return null

        val lanesHeight = componentHeight - axisHeight
        val laneHeight = lanesHeight / lanes.size
        val laneIndex = ((pos.y - axisHeight) / laneHeight).toInt().coerceIn(0, lanes.lastIndex)
        val seconds = (scrollOffset + pos.x) / pixelsPerSecond

        return lanes[laneIndex].blocks.firstOrNull { block ->
            seconds in block.startSeconds..block.endSeconds
        }
    }

    private fun enforceScrollLimits() {
        if (viewportWidth <= 0) return
        val contentWidth = totalSeconds * pixelsPerSecond
        val maxScroll = max(0f, contentWidth - viewportWidth)

        if (scrollOffset < 0f) scrollOffset = 0f
        if (scrollOffset > maxScroll) scrollOffset = maxScroll
    }
}

/**
 * Dobiera interwał ticków (w sekundach) na podstawie aktualnego zoomu,
 * tak żeby odstęp między liniami wynosił co najmniej [minPixelSpacing] px.
 * Minimalny interwał = 60s (co minutę).
 */
fun pickTickInterval(pixelsPerSecond: Float, minPixelSpacing: Float = 40f): Int {
    val intervals = listOf(3600, 1800, 900, 600, 300, 60)
    return intervals.lastOrNull { it * pixelsPerSecond >= minPixelSpacing } ?: 3600
}

fun formatTickLabel(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return "%02d:%02d".format(h, m)
}

fun formatTimeSeconds(seconds: Float): String {
    val totalSec = seconds.toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

fun formatDuration(seconds: Float): String {
    val totalSec = seconds.toInt()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return when {
        h > 0 -> "${h}h ${m}min ${s}s"
        m > 0 -> "${m}min ${s}s"
        else -> "${s}s"
    }
}

@Composable
fun rememberTimelineState(): TimelineState {
    return remember { TimelineState() }
}

@Composable
fun rememberCurrentTimeSeconds(enabled: Boolean): Float {
    var seconds by remember { mutableStateOf(currentTimeAsSeconds()) }

    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            seconds = currentTimeAsSeconds()
            delay(3_000)
        }
    }

    return seconds
}

private fun currentTimeAsSeconds(): Float {
    val now = LocalTime.now()
    return now.hour * 3600f + now.minute * 60f + now.second
}
