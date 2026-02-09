package org.octavius.ui.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.max

class TimelineState {
    val totalSeconds = 86400 // 24h * 60min * 60s

    var pixelsPerSecond by mutableStateOf(0f)
        private set

    var scrollOffset by mutableStateOf(0f)
        private set

    /** Czas w sekundach pod kursorem, null gdy kursor poza komponentem */
    var hoverSeconds by mutableStateOf<Float?>(null)

    private var viewportWidth = 1f

    fun updateViewportWidth(width: Float) {
        if (width == viewportWidth) return
        viewportWidth = width
        enforceConstraints()
    }

    fun onHoverMove(mouseX: Float) {
        if (pixelsPerSecond <= 0f) return
        val sec = (scrollOffset + mouseX) / pixelsPerSecond
        hoverSeconds = sec.coerceIn(0f, totalSeconds.toFloat())
    }

    fun onHoverExit() {
        hoverSeconds = null
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
