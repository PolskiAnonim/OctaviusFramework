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
    state: TimelineState = rememberTimelineState(),
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            // Przekazujemy zdarzenia myszy do stanu
            .onPointerEvent(PointerEventType.Scroll) {
                val change = it.changes.first()
                state.onPointerEvent(change.scrollDelta.x, change.scrollDelta.y)
                change.consume()
            }
    ) {
        // Aktualizujemy wiedzę stanu o szerokości okna
        val width = constraints.maxWidth.toFloat()
        SideEffect {
            state.updateViewportWidth(width)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pxPerMinute = state.pixelsPerMinute
            val scrollX = state.scrollOffset

            // Przesunięcie całego rysowania
            translate(left = -scrollX) {

                // Optymalizacja: wyliczamy widoczny zakres minut
                val startMinute = (scrollX / pxPerMinute).toInt().coerceAtLeast(0)
                val endMinute = ((scrollX + width) / pxPerMinute).toInt().coerceAtMost(1440)

                // Rysowanie godzin
                for (h in 0..24) {
                    val min = h * 60
                    // Rysujemy tylko to co widać + margines 60min
                    if (min in (startMinute - 60)..(endMinute + 60)) {
                        val x = min * pxPerMinute

                        // Linia godziny
                        drawLine(
                            color = Color.Gray,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )

                        // Tekst godziny
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "%02d:00".format(h),
                            topLeft = Offset(x + 5f, 5f),
                            style = TextStyle(color = Color.LightGray)
                        )
                    }
                }

                // Czerwona linia końca doby (24:00)
                val endX = 1440 * pxPerMinute
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