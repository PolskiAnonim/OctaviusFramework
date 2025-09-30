package org.octavius.modules.games.statistics.model

import org.octavius.data.PgType
import org.octavius.domain.game.GameStatus
import java.math.BigDecimal

// Typ kompozytowy dla rozkładu statusów
@PgType
data class DashboardStatusCount(
    val status: GameStatus,
    val count: Long
)

// Typ kompozytowy dla gry w liście "najwięcej grane"
@PgType
data class DashboardGameByTime(
    val id: Int,
    val name: String,
    val playTimeHours: BigDecimal
)

// Typ kompozytowy dla gry w liście "najwyżej oceniane"
@PgType
data class DashboardGameByRating(
    val id: Int,
    val name: String,
    val averageRating: BigDecimal
)

// Główna data class, która mapuje się na wynik całego zapytania
data class GameStatisticsData(
    val totalGames: Long,
    val totalPlaytimeHours: BigDecimal,
    val playedGamesCount: Long,
    val avgPlaytimeForPlayed: BigDecimal,
    val statusDistribution: List<DashboardStatusCount>?,
    val avgStoryRating: BigDecimal?,
    val avgGameplayRating: BigDecimal?,
    val avgAtmosphereRating: BigDecimal?,
    val mostPlayedGames: List<DashboardGameByTime>?,
    val highestRatedGames: List<DashboardGameByRating>?,
    val favoriteCategoryName: String?,
    val favoriteSeriesName: String?
)

// Stan, którego będzie używać UI
data class GameStatisticsState(
    val data: GameStatisticsData? = null,
    val isLoading: Boolean = true
)