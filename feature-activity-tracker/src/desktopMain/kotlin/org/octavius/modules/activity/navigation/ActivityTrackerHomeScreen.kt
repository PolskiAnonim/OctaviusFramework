package org.octavius.modules.activity.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.modules.activity.report.ui.ActivityLogReportScreen
import org.octavius.modules.activity.report.ui.CategoriesReportScreen
import org.octavius.modules.activity.report.ui.DocumentsReportScreen
import org.octavius.modules.activity.report.ui.RulesReportScreen
import org.octavius.modules.activity.statistics.ui.StatisticsScreen
import org.octavius.modules.activity.timeline.UnifiedTimelineState
import org.octavius.modules.activity.timeline.ui.UnifiedTimelineScreen
import org.octavius.modules.activity.weekly.ui.WeeklyTrendScreen
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Screen

class ActivityTrackerHomeScreen : Screen {

    override val title = Tr.ActivityTracker.Home.title()

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = Tr.ActivityTracker.Home.mainText(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Tr.ActivityTracker.Home.description(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions Grid - Row 1: Configuration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    title = Tr.ActivityTracker.Home.categories(),
                    description = Tr.ActivityTracker.Home.categoriesDescription(),
                    icon = Icons.Default.Category,
                    onClick = { AppRouter.navigateTo(CategoriesReportScreen.create()) },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = Tr.ActivityTracker.Home.rules(),
                    description = Tr.ActivityTracker.Home.rulesDescription(),
                    icon = Icons.Default.Rule,
                    onClick = { AppRouter.navigateTo(RulesReportScreen.create()) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Reports
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    title = Tr.ActivityTracker.Home.activityLog(),
                    description = Tr.ActivityTracker.Home.activityLogDescription(),
                    icon = Icons.Default.History,
                    onClick = { AppRouter.navigateTo(ActivityLogReportScreen.create()) },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = Tr.ActivityTracker.Home.documents(),
                    description = Tr.ActivityTracker.Home.documentsDescription(),
                    icon = Icons.Default.Description,
                    onClick = { AppRouter.navigateTo(DocumentsReportScreen.create()) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Timelines
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    title = Tr.ActivityTracker.Home.appTimeline(),
                    description = Tr.ActivityTracker.Home.appTimelineDescription(),
                    icon = Icons.Default.Timeline,
                    onClick = { AppRouter.navigateTo(UnifiedTimelineScreen()) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    title = Tr.ActivityTracker.Home.statistics(),
                    description = Tr.ActivityTracker.Home.statisticsDescription(),
                    icon = Icons.Default.PieChart,
                    onClick = { AppRouter.navigateTo(StatisticsScreen.create()) },
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = Tr.ActivityTracker.Home.weeklyTrend(),
                    description = Tr.ActivityTracker.Home.weeklyTrendDescription(),
                    icon = Icons.Default.BarChart,
                    onClick = { AppRouter.navigateTo(WeeklyTrendScreen.create()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    companion object {
        fun create(): ActivityTrackerHomeScreen = ActivityTrackerHomeScreen()
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
