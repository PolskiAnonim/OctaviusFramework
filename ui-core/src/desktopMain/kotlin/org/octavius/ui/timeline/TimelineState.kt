package org.octavius.ui.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.max

class TimelineState {
    private val totalMinutes = 1440 // 24h * 60min

    var pixelsPerMinute by mutableStateOf(1f)
        private set

    var scrollOffset by mutableStateOf(0f)
        private set

    private var viewportWidth = 1f

    fun updateViewportWidth(width: Float) {
        if (width == viewportWidth) return
        viewportWidth = width
        enforceConstraints()
    }

    /**
     * Obsługa zdarzenia z uwzględnieniem pozycji myszki dla zooma
     */
    fun onPointerEvent(deltaX: Float, deltaY: Float, mouseX: Float) {
        // Zoom (kółko góra/dół)
        if (deltaY != 0f && deltaX == 0f) {
            // Odwrócona logika (standardowo scroll w dół to zoom out)
            val zoomFactor = if (deltaY > 0) 0.9f else 1.1f
            applyZoom(zoomFactor, mouseX)
        }
        // Scroll (Shift + kółko lub touchpad bok)
        else {
            val moveDelta = if (deltaX != 0f) deltaX else deltaY
            applyScroll(moveDelta)
        }
    }

    private fun applyZoom(factor: Float, pivotX: Float) {
        val oldPx = pixelsPerMinute

        // 1. Obliczamy, która minuta jest aktualnie pod kursorem myszki
        //    (offset + pozycja_myszy) / zoom = czas_pod_myszką
        val timeAtMouse = (scrollOffset + pivotX) / oldPx

        // 2. Obliczamy nowy zoom (z ograniczeniami)
        var newPx = oldPx * factor

        // --- Sprawdzenie constraintów zooma "na brudno" przed przypisaniem ---
        if (viewportWidth > 0) {
            val minPx = viewportWidth / totalMinutes
            val maxPx = 50f // Max zoom
            newPx = newPx.coerceIn(minPx, maxPx)
        }
        pixelsPerMinute = newPx

        // 3. Przeliczamy scroll tak, aby 'timeAtMouse' nadal był pod 'pivotX'
        //    nowy_offset = (czas * nowy_zoom) - pozycja_myszy
        scrollOffset = (timeAtMouse * newPx) - pivotX

        // 4. Na koniec upewniamy się, że scroll nie wyjechał poza granice 0..24h
        enforceScrollLimits()
    }

    private fun applyScroll(delta: Float) {
        val scrollSpeed = 25f
        scrollOffset += (delta * scrollSpeed)
        enforceScrollLimits()
    }

    private fun enforceConstraints() {
        if (viewportWidth <= 0) return
        // Minimalny zoom żeby wypełnić ekran
        val minPx = viewportWidth / totalMinutes
        if (pixelsPerMinute < minPx) pixelsPerMinute = minPx
        enforceScrollLimits()
    }

    private fun enforceScrollLimits() {
        if (viewportWidth <= 0) return
        val contentWidth = totalMinutes * pixelsPerMinute
        val maxScroll = max(0f, contentWidth - viewportWidth)

        if (scrollOffset < 0f) scrollOffset = 0f
        if (scrollOffset > maxScroll) scrollOffset = maxScroll
    }
}

@Composable
fun rememberTimelineState(): TimelineState {
    return remember { TimelineState() }
}