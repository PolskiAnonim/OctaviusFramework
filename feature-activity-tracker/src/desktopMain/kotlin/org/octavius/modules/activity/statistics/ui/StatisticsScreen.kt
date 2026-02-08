package org.octavius.modules.activity.statistics.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.octavius.localization.Tr
import org.octavius.modules.activity.statistics.StatisticsHandler
import org.octavius.modules.activity.statistics.StatisticsState
import org.octavius.navigation.Screen
import kotlin.time.Clock

class StatisticsScreen : Screen {
    override val title = Tr.ActivityTracker.Statistics.title()

    @Composable
    override fun Content() {
        val handler = remember { StatisticsHandler() }
        val today = remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }

        var state by remember {
            mutableStateOf(StatisticsState(selectedDate = today))
        }

        LaunchedEffect(state.selectedDate) {
            state = state.copy(isLoading = true)
            val slices = withContext(Dispatchers.IO) {
                handler.loadCategoryBreakdown(state.selectedDate)
            }
            val topApps = withContext(Dispatchers.IO) {
                handler.loadTopApplications(state.selectedDate)
            }
            val totalTime = withContext(Dispatchers.IO) {
                handler.loadTotalTime(state.selectedDate)
            }
            state = state.copy(
                slices = slices,
                topApplications = topApps,
                totalSeconds = totalTime,
                isLoading = false
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            state = state.copy(
                                selectedDate = state.selectedDate.minus(DatePeriod(days = 1))
                            )
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.selectedDate.dayOfMonth}.${state.selectedDate.monthNumber}.${state.selectedDate.year}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${Tr.ActivityTracker.Statistics.totalTime()}: ${formatDuration(state.totalSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            state = state.copy(
                                selectedDate = state.selectedDate.plus(DatePeriod(days = 1))
                            )
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.slices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Tr.ActivityTracker.Statistics.noData(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Category Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Tr.ActivityTracker.Statistics.categoryBreakdown(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CategoryPieChart(
                            slices = state.slices,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Top Applications
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Tr.ActivityTracker.Statistics.topApplications(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        state.topApplications.forEachIndexed { index, app ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column {
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        app.categoryName?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = formatDuration(app.totalSeconds),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (index < state.topApplications.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    companion object {
        fun create(): StatisticsScreen = StatisticsScreen()
    }
}
