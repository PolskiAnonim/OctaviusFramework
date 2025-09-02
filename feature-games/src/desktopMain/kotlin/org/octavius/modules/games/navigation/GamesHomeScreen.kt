package org.octavius.modules.games.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.octavius.localization.T
import org.octavius.modules.games.report.ui.GameReportScreen
import org.octavius.modules.games.report.ui.GameSeriesReportScreen
import org.octavius.modules.games.report.ui.GameCategoriesReportScreen
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Screen

/**
 * Ekran domyślny dla modułu Games - wyświetla podsumowanie i linki do głównych funkcji.
 */
class GamesHomeScreen : Screen {
    
    override val title = T.get("games.home.title")

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
                        text = T.get("games.home.mainText"),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = T.get("games.home.description"),
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
                    title = T.get("games.home.allGames"),
                    description = T.get("games.home.allGamesDescription"),
                    icon = Icons.Default.SportsEsports,
                    onClick = {
                        AppRouter.navigateTo(GameReportScreen.create())
                    },
                    modifier = Modifier.weight(1f)
                )

                // Game Series
                QuickActionCard(
                    title = T.get("games.home.gameSeries"),
                    description = T.get("games.home.gameSeriesDescription"),
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
                    title = T.get("games.home.gameCategories"),
                    description = T.get("games.home.gameCategoriesDescription"),
                    icon = Icons.Default.Category,
                    onClick = {
                        AppRouter.navigateTo(GameCategoriesReportScreen.create())
                    },
                    modifier = Modifier.weight(1f)
                )

                // Statistics (placeholder for future)
                QuickActionCard(
                    title = T.get("games.home.statistics"),
                    description = T.get("games.home.statisticsDescription"),
                    icon = Icons.Default.Analytics,
                    onClick = {
                        // TODO: Navigate to Statistics screen
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