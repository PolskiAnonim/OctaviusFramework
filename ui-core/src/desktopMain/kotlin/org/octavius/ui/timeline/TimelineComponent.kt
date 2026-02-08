package org.octavius.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineComponent(
    modifier: Modifier = Modifier
) {
    val totalMinutes = 1440 // 24h * 60min

    var pxPerMinute by remember { mutableStateOf(1f) }
    var scrollX by remember { mutableStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val viewportWidth = constraints.maxWidth.toFloat()

        // KLUCZOWA POPRAWKA:
        // Obliczamy minimalny zoom, przy którym 24h zajmuje dokładnie całą szerokość ekranu.
        // Nie pozwalamy, aby pxPerMinute był mniejszy niż ta wartość.
        val minPxPerMinute = if (viewportWidth > 0) viewportWidth / totalMinutes else 0.1f

        // Jeśli okno zostało powiększone, a zoom był stary, podbijamy go natychmiast
        if (pxPerMinute < minPxPerMinute) {
            pxPerMinute = minPxPerMinute
        }

        val contentWidth = totalMinutes * pxPerMinute

        // Max scroll to nadmiar szerokości contentu ponad szerokość okna
        val maxScroll = max(0f, contentWidth - viewportWidth)

        // Hard clamp: scroll nigdy nie może wyjść poza wyliczony zakres
        if (scrollX > maxScroll) scrollX = maxScroll
        if (scrollX < 0f) scrollX = 0f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val change = event.changes.first()
                    val dx = change.scrollDelta.x
                    val dy = change.scrollDelta.y

                    // 1. ZOOM (Kółko góra/dół, czyli dy)
                    // W Compose Desktop często zwykły scroll ma dx=0, dy!=0
                    if (dy != 0f && dx == 0f) {
                        val zoomFactor = if (dy > 0) 0.9f else 1.1f
                        val newPx = pxPerMinute * zoomFactor

                        // Obliczamy punkt pod kursorem, żeby zoomować "do myszki" (opcjonalne, ale wygodne)
                        // Tutaj prościej: zachowujemy relatywną pozycję środka
                        val centerRatio = (scrollX + viewportWidth / 2) / contentWidth

                        // Aplikujemy nowy zoom, ale NIE MNIEJSZY niż minimalny (żeby nie było pustego po prawej)
                        pxPerMinute = newPx.coerceAtLeast(minPxPerMinute)

                        // Przeliczamy scroll po zoomie
                        val newContentWidth = totalMinutes * pxPerMinute
                        val newMaxScroll = max(0f, newContentWidth - viewportWidth)
                        scrollX = (centerRatio * newContentWidth - viewportWidth / 2).coerceIn(0f, newMaxScroll)
                    }
                    // 2. PRZEWIJANIE (Shift+Scroll lub Touchpad w bok, czyli dx)
                    else {
                        val scrollSpeed = 25f
                        // Bierzemy dx, a jak go nie ma (niektóre myszki), to awaryjnie dy
                        val delta = if (dx != 0f) dx else dy

                        val newScroll = scrollX + (delta * scrollSpeed)
                        scrollX = newScroll.coerceIn(0f, maxScroll)
                    }

                    change.consume()
                }
        ) {
            translate(left = -scrollX) {

                // Rysujemy tylko widoczny fragment + margines
                val startMinute = (scrollX / pxPerMinute).toInt().coerceAtLeast(0)
                val endMinute = ((scrollX + viewportWidth) / pxPerMinute).toInt().coerceAtMost(totalMinutes)

                // Główna pętla rysowania
                for (h in 0..24) {
                    val min = h * 60
                    if (min in (startMinute - 60)..(endMinute + 60)) {
                        val x = min * pxPerMinute

                        // Linia
                        drawLine(
                            color = Color.Gray,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )

                        // Tekst
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "%02d:00".format(h),
                            topLeft = Offset(x + 5f, 5f),
                            style = TextStyle(color = Color.LightGray)
                        )
                    }
                }

                // Wyraźna czerwona krecha na 24:00 (koniec doby)
                val endX = totalMinutes * pxPerMinute
                drawLine(
                    color = Color.Red,
                    start = Offset(endX, 0f),
                    end = Offset(endX, size.height),
                    strokeWidth = 2f
                )
            }
        }
    }
}