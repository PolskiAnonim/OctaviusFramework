package org.octavius.modules.games.statistics.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.localization.T
import org.octavius.modules.games.form.game.ui.GameFormScreen
import org.octavius.modules.games.statistics.model.DashboardGame
import org.octavius.modules.games.statistics.model.DashboardGameByRating
import org.octavius.modules.games.statistics.model.DashboardGameByTime
import org.octavius.modules.games.statistics.model.GameStatisticsData
import org.octavius.modules.games.statistics.model.GameStatisticsHandler
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Screen
import java.math.BigDecimal

class GameStatisticsScreen(override val title: String) : Screen {
    @Composable
    override fun Content() {
        val composableScope = rememberCoroutineScope()

        val handler = remember(composableScope) {
            GameStatisticsHandler(composableScope)
        }
        val state by handler.state.collectAsState()

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.data == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(T.get("statistics.noData"))
            }
        } else {
            val data = state.data!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { KpiStats(data) }
                item { RatingStats(data) }
                item { FavoriteStats(data) }

                item {
                    QuickAccessGameList(
                        title = T.get("games.stats.mostPlayed"),
                        items = data.mostPlayedGames.orEmpty(),
                        onItemClick = { AppRouter.navigateTo(GameFormScreen.create(it)) }
                    )
                }

                item {
                    QuickAccessGameList(
                        title = T.get("games.stats.highestRated"),
                        items = data.highestRatedGames.orEmpty(),
                        onItemClick = { AppRouter.navigateTo(GameFormScreen.create(it)) }
                    )
                }
            }
        }
    }

    companion object {
        fun create(): Screen {
            return GameStatisticsScreen(T.get("games.stats.title"))
        }
    }
}


@Composable
private fun KpiStats(data: GameStatisticsData) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(T.get("games.stats.kpiHeader"), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(T.get("games.stats.totalGames"), data.totalGames.toString(), Modifier.weight(1f))
            StatCard(T.get("games.stats.playedGames"), data.playedGamesCount.toString(), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                T.get("games.stats.totalPlaytime"),
                "${data.totalPlaytimeHours}h",
                Modifier.weight(1f)
            )
            StatCard(
                T.get("games.stats.avgPlaytime"),
                "${data.avgPlaytimeForPlayed}h",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RatingStats(data: GameStatisticsData) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(T.get("games.stats.ratingHeader"), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(T.get("games.stats.avgStory"), data.avgStoryRating?.toString() ?: "-", Modifier.weight(1f))
            StatCard(T.get("games.stats.avgGameplay"), data.avgGameplayRating?.toString() ?: "-", Modifier.weight(1f))
            StatCard(T.get("games.stats.avgAtmosphere"), data.avgAtmosphereRating?.toString() ?: "-", Modifier.weight(1f))
        }
    }
}

@Composable
private fun FavoriteStats(data: GameStatisticsData) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(T.get("games.stats.favoritesHeader"), style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(T.get("games.stats.favoriteCategory"), data.favoriteCategoryName ?: "-", Modifier.weight(1f))
            StatCard(T.get("games.stats.favoriteSeries"), data.favoriteSeriesName ?: "-", Modifier.weight(1f))
        }
    }
}


@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Text(text = title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable
private fun QuickAccessGameList(
    title: String,
    items: List<DashboardGame>,
    onItemClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (items.isEmpty()) {
            Text(T.get("statistics.noItemsInList"), style = MaterialTheme.typography.bodyMedium)
        } else {
            Card {
                Column {
                    items.forEachIndexed { index, item ->

                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text(item.value) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { onItemClick(item.id) }
                        )
                        if (index < items.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}