package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.octavius.localization.Tr
import org.octavius.modules.activity.timeline.DocumentTimelineEntry
import org.octavius.modules.activity.timeline.DocumentTimelineState
import org.octavius.modules.activity.timeline.TimelineHandler
import org.octavius.navigation.Screen
import kotlin.time.Clock

class DocumentTimelineScreen : Screen {
    override val title = Tr.ActivityTracker.Timeline.docTimelineTitle()

    @Composable
    override fun Content() {
        val handler = remember { TimelineHandler() }
        val today = remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }

        var state by remember {
            mutableStateOf(DocumentTimelineState(selectedDate = today))
        }

        LaunchedEffect(state.selectedDate) {
            state = state.copy(isLoading = true)
            val entries = withContext(Dispatchers.IO) {
                handler.loadDocuments(state.selectedDate)
            }
            state = state.copy(entries = entries, isLoading = false)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TimelineControls(
                selectedDate = state.selectedDate,
                zoomLevel = state.zoomLevel,
                onDateChange = { newDate ->
                    state = state.copy(selectedDate = newDate)
                },
                onZoomChange = { newZoom ->
                    state = state.copy(zoomLevel = newZoom)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Tr.ActivityTracker.Timeline.noData(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val textMeasurer = rememberTextMeasurer()
                    val gridColor = Color.Gray.copy(alpha = 0.3f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .padding(16.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val hourWidth = state.zoomLevel

                        // Draw hour grid
                        for (hour in 0..24) {
                            val x = (hour * hourWidth).coerceAtMost(canvasWidth)
                            drawLine(
                                color = gridColor,
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 1f
                            )

                            if (hour < 24) {
                                val label = String.format("%02d:00", hour)
                                val textLayout = textMeasurer.measure(
                                    text = label,
                                    style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                                )
                                drawText(
                                    textLayoutResult = textLayout,
                                    topLeft = Offset(x + 4, canvasHeight - 16)
                                )
                            }
                        }

                        // Draw document markers
                        state.entries.forEach { entry ->
                            val hour = entry.timestamp.hour + entry.timestamp.minute / 60f
                            val x = hour * hourWidth
                            val color = parseColor(entry.color)

                            // Draw diamond marker
                            drawRect(
                                color = color,
                                topLeft = Offset(x - 6, canvasHeight / 2 - 6),
                                size = Size(12f, 12f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Document list
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.entries.forEach { entry ->
                            DocumentListItem(entry)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DocumentListItem(entry: DocumentTimelineEntry) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title ?: entry.path.substringAfterLast("/").substringAfterLast("\\"),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = "${entry.type} - ${entry.path}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = formatTime(entry.timestamp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    private fun formatTime(time: kotlinx.datetime.LocalDateTime): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    private fun parseColor(colorString: String): Color {
        return try {
            val color = colorString.removePrefix("#")
            val colorInt = when (color.length) {
                6 -> color.toLong(16).toInt() or 0xFF000000.toInt()
                8 -> color.toLong(16).toInt()
                else -> 0xFF6366F1.toInt()
            }
            Color(
                red = (colorInt shr 16 and 0xFF) / 255f,
                green = (colorInt shr 8 and 0xFF) / 255f,
                blue = (colorInt and 0xFF) / 255f,
                alpha = 1f
            )
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }
    }

    companion object {
        fun create(): DocumentTimelineScreen = DocumentTimelineScreen()
    }
}
