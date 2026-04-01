package org.octavius.modules.activity.weekly.ui

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
import org.octavius.modules.activity.weekly.WeeklyHandler
import org.octavius.modules.activity.weekly.WeeklyState
import org.octavius.navigation.Screen
import kotlin.time.Clock

class WeeklyTrendScreen : Screen {
    override val title = Tr.ActivityTracker.Weekly.title()

    @Composable
    override fun Content() {
        val handler = remember { WeeklyHandler() }
        val today = remember {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
        val initialWeekStart = remember { handler.getWeekStart(today) }

        var state by remember {
            mutableStateOf(WeeklyState(weekStartDate = initialWeekStart))
        }

        LaunchedEffect(state.weekStartDate) {
            state = state.copy(isLoading = true)
            val days = withContext(Dispatchers.IO) {
                handler.loadWeekData(state.weekStartDate)
            }
            val allCategories = handler.getAllCategories(days)
            val maxSeconds = days.maxOfOrNull { it.totalSeconds } ?: 0L
            state = state.copy(
                days = days,
                allCategories = allCategories,
                maxSeconds = maxSeconds,
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
            // Week navigation
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
                                weekStartDate = state.weekStartDate.minus(DatePeriod(days = 7))
                            )
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
                    }

                    val weekEnd = state.weekStartDate.plus(DatePeriod(days = 6))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.weekStartDate.dayOfMonth}.${state.weekStartDate.monthNumber} - ${weekEnd.dayOfMonth}.${weekEnd.monthNumber}.${weekEnd.year}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        val totalHours = state.days.sumOf { it.totalSeconds } / 3600f
                        Text(
                            text = "${Tr.ActivityTracker.Weekly.totalHours()}: ${String.format("%.1f", totalHours)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            state = state.copy(
                                weekStartDate = state.weekStartDate.plus(DatePeriod(days = 7))
                            )
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
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
            } else if (state.days.all { it.totalSeconds == 0L }) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Tr.ActivityTracker.Weekly.noData(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Stacked Bar Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WeeklyStackedBar(
                            days = state.days,
                            maxSeconds = state.maxSeconds,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Legend
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Tr.ActivityTracker.Weekly.legend(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        WeeklyLegend(
                            categories = state.allCategories,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Daily breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        state.days.forEach { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${day.dayOfWeek} (${day.date.dayOfMonth}.${day.date.monthNumber})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatDuration(day.totalSeconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (day != state.days.last()) {
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
        fun create(): WeeklyTrendScreen = WeeklyTrendScreen()
    }
}
