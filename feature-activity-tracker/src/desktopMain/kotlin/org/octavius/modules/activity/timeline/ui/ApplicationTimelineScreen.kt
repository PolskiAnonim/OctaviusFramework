package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.octavius.modules.activity.timeline.*
import org.octavius.navigation.Screen
import kotlin.time.Clock

class UnifiedTimelineScreen: Screen {

    override val title: String = "Unified"

    @Composable
    override fun Content() {
        val handler = remember { TimelineHandler() }
        val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

        var state by remember { mutableStateOf(UnifiedTimelineState(selectedDate = today)) }

        // Ładowanie danych
        LaunchedEffect(state.selectedDate) {
            state = state.copy(isLoading = true)
            val loadedState = withContext(Dispatchers.IO) {
                handler.loadDailyData(state.selectedDate)
            }
            state = loadedState.copy(zoomLevel = state.zoomLevel) // zachowaj zoom
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
            // 1. Kontrolki (Data, Zoom)
            TimelineControls(
                selectedDate = state.selectedDate,
                zoomLevel = state.zoomLevel,
                onDateChange = { state = state.copy(selectedDate = it) },
                onZoomChange = { state = state.copy(zoomLevel = it) }
            )

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 2. GŁÓWNY OBSZAR TIMELINE'U
            // ScrollState jest współdzielony przez wszystkie pasy
            val scrollState = rememberScrollState()

            // Obliczamy szerokość canvasu na podstawie zoomu (24h * pixelsPerHour)
            val totalWidth = (24 * state.zoomLevel).dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Kontener przewijany horyzontalnie
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(scrollState)
                        .width(totalWidth) // Wymuszamy szerokość, żeby scroll działał
                ) {

                    // A. Linijka Czasu (Top Ruler)
                    TimeRuler(zoomLevel = state.zoomLevel, height = 30.dp)

                    Spacer(modifier = Modifier.height(4.dp))

                    // B. Pas Kategorii (Wysoki, jak na screenie ManicTime)
                    TimelineLaneLabel(text = "Znaczniki (Kategorie)")
                    BlockTimelineLane(
                        blocks = state.categoryBlocks,
                        zoomLevel = state.zoomLevel,
                        height = 60.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // C. Pas Aplikacji
                    TimelineLaneLabel(text = "Aplikacje")
                    BlockTimelineLane(
                        blocks = state.appBlocks,
                        zoomLevel = state.zoomLevel,
                        height = 40.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // D. Pas Dokumentów (Pionowe kreski)
                    TimelineLaneLabel(text = "Dokumenty")
                    DocumentTimelineLane(
                        points = state.documentPoints,
                        zoomLevel = state.zoomLevel,
                        height = 60.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Opcjonalnie: Kursor "Teraz" (pionowa linia przez wszystko)
                CurrentTimeIndicator(state.zoomLevel, scrollState)
            }
        }
    }

    @Composable
    fun TimelineLaneLabel(text: String) {
        Text(
            text = text,
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
    }

    // Rysuje prostokątne bloki (Kategorie i Aplikacje)
    @Composable
    fun BlockTimelineLane(
        blocks: List<TimeBlock>,
        zoomLevel: Float,
        height: androidx.compose.ui.unit.Dp,
        modifier: Modifier
    ) {
        Canvas(modifier = modifier.height(height).background(Color(0xFF2D2D2D))) {
            val laneHeight = size.height

            blocks.forEach { block ->
                val startHour = block.startTime.hour + block.startTime.minute / 60f + block.startTime.second / 3600f
                val endHour = block.endTime.hour + block.endTime.minute / 60f + block.endTime.second / 3600f

                // Zapobieganie zerowej szerokości dla bardzo krótkich zdarzeń
                val durationHours = (endHour - startHour).coerceAtLeast(1f / zoomLevel) // min 1px

                val x = startHour * zoomLevel
                val width = durationHours * zoomLevel

                drawRect(
                    color = parseColor(block.color),
                    topLeft = Offset(x, 0f),
                    size = Size(width, laneHeight)
                )
            }

            // Grid lines (godziny)
            drawGridLines(zoomLevel, laneHeight)
        }
    }

    // Rysuje pionowe kreski (Dokumenty)
    @Composable
    fun DocumentTimelineLane(
        points: List<TimePoint>,
        zoomLevel: Float,
        height: androidx.compose.ui.unit.Dp,
        modifier: Modifier
    ) {
        Canvas(modifier = modifier.height(height).background(Color(0xFF2D2D2D))) {
            val laneHeight = size.height

            // Rysujemy tło pasów (naprzemienne kolory co godzinę dla czytelności - opcjonalne)
            // ...

            points.forEach { point ->
                val hour = point.timestamp.hour + point.timestamp.minute / 60f + point.timestamp.second / 3600f
                val x = hour * zoomLevel

                // Rysowanie linii dokumentu
                drawLine(
                    color = parseColor(point.color),
                    start = Offset(x, 0f),
                    end = Offset(x, laneHeight),
                    strokeWidth = 2f // grubsza linia dla widoczności
                )
            }

            drawGridLines(zoomLevel, laneHeight)
        }
    }

    @Composable
    fun TimeRuler(zoomLevel: Float, height: androidx.compose.ui.unit.Dp) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            for (h in 0..24) {
                val x = h * zoomLevel

                // Główna kreska godziny
                drawLine(
                    color = Color.Gray,
                    start = Offset(x, size.height - 10),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )

                // Tekst godziny
                val textLayout = textMeasurer.measure(
                    text = String.format("%02d:00", h),
                    style = TextStyle(color = Color.LightGray, fontSize = 10.sp)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(x + 4, 0f)
                )

                // Półgodziny (opcjonalnie)
                if (zoomLevel > 50) {
                    val xHalf = (h + 0.5f) * zoomLevel
                    drawLine(
                        color = Color.DarkGray,
                        start = Offset(xHalf, size.height - 5),
                        end = Offset(xHalf, size.height),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }

    // Funkcja pomocnicza do rysowania pionowych linii siatki na pasach
    fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLines(zoomLevel: Float, height: Float) {
        for (h in 0..24) {
            val x = h * zoomLevel
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }
    }

    @Composable
    fun CurrentTimeIndicator(zoomLevel: Float, scrollState: ScrollState) {
        // To jest tylko wizualny bajer pokazujący aktualny czas
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = now.hour + now.minute / 60f

        // Potrzebujemy offsetu, bo Box nadrzędny nie scrolluje, scrolluje Column w środku.
        // Żeby narysować linię statycznie nad scrollem, musimy odjąć scrollState.value
        // ALE lepiej umieścić to wewnątrz Column (wtedy scrolluje się razem z treścią)
        // lub użyć skomplikowanej matematyki.
        // Najprościej: narysować czerwoną linię na każdym Canvasie w drawGridLines :)
    }

    // Helper do kolorów
    fun parseColor(hex: String): Color {
        return try {
            val clean = hex.removePrefix("#")
            val colorInt = when (clean.length) {
                6 -> clean.toLong(16).toInt() or 0xFF000000.toInt()
                8 -> clean.toLong(16).toInt()
                else -> 0xFF6366F1.toInt()
            }
            Color(colorInt)
        } catch (e: Exception) {
            Color.Gray
        }
    }
}