package org.octavius.modules.games.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.modules.games.report.ui.GameCategoriesReportScreen
import org.octavius.modules.games.report.ui.GameReportScreen
import org.octavius.modules.games.report.ui.GameSeriesReportScreen
import org.octavius.modules.games.statistics.ui.GameStatisticsScreen
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Screen

/**
 * Ekran domyślny dla modułu Games - wyświetla podsumowanie i linki do głównych funkcji.
 */
class GamesHomeScreen : Screen {

    override val title = Tr.Games.Home.title()

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
                        text = Tr.Games.Home.mainText(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = Tr.Games.Home.description(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // All Games
                QuickActionCard(
                    title = Tr.Games.Home.allGames(),
                    description = Tr.Games.Home.allGamesDescription(),
                    icon = Icons.Default.SportsEsports,
                    onClick = {
                        AppRouter.navigateTo(GameReportScreen.create())
                    },
                    modifier = Modifier.weight(1f)
                )

                // Game Series
                QuickActionCard(
                    title = Tr.Games.Home.gameSeries(),
                    description = Tr.Games.Home.gameSeriesDescription(),
                    icon = Icons.Default.CollectionsBookmark,
                    onClick = {
                        AppRouter.navigateTo(GameSeriesReportScreen.create())
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game Categories
                QuickActionCard(
                    title = Tr.Games.Home.gameCategories(),
                    description = Tr.Games.Home.gameCategoriesDescription(),
                    icon = Icons.Default.Category,
                    onClick = {
                        AppRouter.navigateTo(GameCategoriesReportScreen.create())
                    },
                    modifier = Modifier.weight(1f)
                )

                // Statistics (placeholder for future)
                QuickActionCard(
                    title = Tr.Games.Home.statistics(),
                    description = Tr.Games.Home.statisticsDescription(),
                    icon = Icons.Default.Analytics,
                    onClick = {
                        AppRouter.navigateTo(GameStatisticsScreen.create())
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    companion object {
        fun create(): GamesHomeScreen = GamesHomeScreen()
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