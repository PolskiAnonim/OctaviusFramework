package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.octavius.localization.Tr
import org.octavius.modules.activity.timeline.TimelineHandler
import org.octavius.modules.activity.timeline.TimelineState
import org.octavius.navigation.Screen
import kotlin.time.Clock

class ApplicationTimelineScreen : Screen {
    override val title = Tr.ActivityTracker.Timeline.appTimelineTitle()

    @Composable
    override fun Content() {
        val handler = remember { TimelineHandler() }
        val today = remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }

        var state by remember {
            mutableStateOf(TimelineState(selectedDate = today))
        }

        LaunchedEffect(state.selectedDate) {
            state = state.copy(isLoading = true)
            val entries = withContext(Dispatchers.IO) {
                handler.loadActivities(state.selectedDate)
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
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        TimelineCanvas(
                            state = state,
                            onSelectionChange = { range ->
                                state = state.copy(selectionRange = range)
                            }
                        )

                        state.selectionRange?.let { (start, end) ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${Tr.ActivityTracker.Timeline.selectedRange()}: ${formatTimeFromX(start, state.zoomLevel)} - ${formatTimeFromX(end, state.zoomLevel)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Activity list
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
                            ActivityListItem(entry)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ActivityListItem(entry: org.octavius.modules.activity.timeline.TimelineEntry) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = entry.processName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${formatTime(entry.startTime)} - ${entry.endTime?.let { formatTime(it) } ?: "..."}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    private fun formatTime(time: kotlinx.datetime.LocalDateTime): String {
        return String.format("%02d:%02d", time.hour, time.minute)
    }

    private fun formatTimeFromX(x: Float, zoomLevel: Float): String {
        val totalMinutes = (x / zoomLevel * 60).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format("%02d:%02d", hours.coerceIn(0, 23), minutes.coerceIn(0, 59))
    }

    companion object {
        fun create(): ApplicationTimelineScreen = ApplicationTimelineScreen()
    }
}
