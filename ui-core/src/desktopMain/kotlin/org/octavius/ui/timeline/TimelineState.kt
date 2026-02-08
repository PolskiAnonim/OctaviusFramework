package org.octavius.ui.timeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.max

class TimelineState {
    private val totalMinutes = 1440 // 24h * 60min

    // Te zmienne są obserwowalne przez Compose
    var pixelsPerMinute by mutableStateOf(1f)
        private set

    var scrollOffset by mutableStateOf(0f)
        private set

    // Szerokość widoku jest potrzebna do obliczania limitów
    private var viewportWidth = 1f

    /**
     * Aktualizuje szerokość widoku. Wywoływane przez komponent przy zmianie rozmiaru okna.
     * Automatycznie poprawia scroll i zoom, żeby nie "wylecieć" poza zakres.
     */
    fun updateViewportWidth(width: Float) {
        if (width == viewportWidth) return
        viewportWidth = width
        enforceConstraints()
    }

    /**
     * Główna metoda obsługi zdarzeń myszy (scroll i zoom)
     */
    fun onPointerEvent(dx: Float, dy: Float) {
        // Logika rozróżniania scrolla od zooma
        if (dy != 0f && dx == 0f) {
            // ZOOM (kółko góra/dół)
            val zoomChange = if (dy > 0) 0.9f else 1.1f
            applyZoom(zoomChange)
        } else {
            // SCROLL (shift+scroll lub touchpad bok)
            val moveDelta = if (dx != 0f) dx else dy
            applyScroll(moveDelta)
        }
    }

    private fun applyZoom(factor: Float) {
        val oldPx = pixelsPerMinute
        val newPx = oldPx * factor

        // Punkt odniesienia zooma (środek ekranu), żeby zoomować "w miejscu"
        val centerTime = (scrollOffset + viewportWidth / 2) / oldPx

        pixelsPerMinute = newPx
        enforceConstraints() // Najpierw sprawdzamy min/max zoom

        // Po zmianie zooma, staramy się utrzymać ten sam czas na środku ekranu
        val newScrollRaw = (centerTime * pixelsPerMinute) - (viewportWidth / 2)
        scrollOffset = newScrollRaw

        enforceConstraints() // Ponownie sprawdzamy, czy scroll nie wyjechał
    }

    private fun applyScroll(delta: Float) {
        val scrollSpeed = 25f
        scrollOffset += (delta * scrollSpeed)
        enforceConstraints()
    }

    /**
     * Serce logiki: pilnuje granic 0..24h i minimalnego zooma
     */
    private fun enforceConstraints() {
        if (viewportWidth <= 0) return

        // 1. Ograniczenie Zooma (minZoom)
        // Cała doba (1440 min) musi zająć co najmniej szerokość ekranu
        val minPxPerMinute = viewportWidth / totalMinutes
        if (pixelsPerMinute < minPxPerMinute) {
            pixelsPerMinute = minPxPerMinute
        }

        // Ograniczenie maksymalnego zooma (opcjonalne, żeby nie przesadzić)
        val maxPxPerMinute = 50f
        if (pixelsPerMinute > maxPxPerMinute) {
            pixelsPerMinute = maxPxPerMinute
        }

        // 2. Ograniczenie Scrolla (min/max scroll)
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