package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import org.octavius.localization.Tr
import org.octavius.modules.activity.timeline.TimelineHandler
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineComponent
import org.octavius.ui.timeline.TimelineLane
import org.octavius.ui.timeline.rememberTimelineState
import kotlin.time.Clock

class UnifiedTimelineScreen : Screen {

    override val title = Tr.ActivityTracker.Timeline.title()

    @Composable
    override fun Content() {
        val handler = remember { TimelineHandler() }
        val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val timelineState = rememberTimelineState()

        var selectedDate by remember { mutableStateOf(today) }
        var lanes by remember { mutableStateOf<List<TimelineLane>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }

        LaunchedEffect(selectedDate) {
            isLoading = true
            lanes = withContext(Dispatchers.IO) { handler.loadLanes(selectedDate) }
            isLoading = false
        }

        Column(modifier = Modifier.fillMaxSize()) {
            DateNavigationBar(
                selectedDate = selectedDate,
                onPrevious = { selectedDate = selectedDate.minus(DatePeriod(days = 1)) },
                onNext = { selectedDate = selectedDate.plus(DatePeriod(days = 1)) },
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!isLoading && lanes.all { it.blocks.isEmpty() }) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = Tr.ActivityTracker.Timeline.noData(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                TimelineComponent(
                    state = timelineState,
                    showCurrentTime = selectedDate == today,
                    lanes = lanes,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DateNavigationBar(
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }
        Text(
            text = "${selectedDate.dayOfMonth}.${selectedDate.monthNumber}.${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
