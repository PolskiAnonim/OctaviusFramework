package org.octavius.modules.games.statistics.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toSingleOf
import org.octavius.data.exception.DatabaseException
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class GameStatisticsHandler(val scope: CoroutineScope) : KoinComponent {
    private val dataAccess: DataAccess by inject()
    private val _state = MutableStateFlow(GameStatisticsState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun buildGameStatisticsQuery(): String {
        // --- CTE 1: kpi_stats ---
        // Bez zmian, było idealnie.
        val kpiStatsQuery = dataAccess.select(
            "COUNT(*) AS total_games",
            "(SELECT time_played FROM games.time_played) AS total_playtime_hours",
            "COUNT(*) FILTER (WHERE g.status = 'PLAYED') AS played_games_count",
            "COALESCE(AVG(pt.play_time_hours) FILTER (WHERE g.status = 'PLAYED'), 0)::numeric(10, 2) AS avg_playtime_for_played"
        )
            .from("games.games g LEFT JOIN games.play_time pt ON g.id = pt.game_id")
            .toSql()

        // --- CTE 2: status_distribution ---
        // Bez zmian, było idealnie.
        val statusDistributionQuery = dataAccess.select(
            "array_agg(ROW(status, count)::games.dashboard_status_count ORDER BY count DESC) AS distribution"
        )
            .fromSubquery(
                dataAccess.select("status, COUNT(*) as count")
                    .from("games.games")
                    .groupBy("status")
                    .toSql()
            )
            .toSql()

        // --- CTE 3: rating_stats ---
        // Bez zmian.
        val ratingStatsQuery = dataAccess.select(
            "AVG(story_rating)::numeric(10, 2) AS avg_story_rating",
            "AVG(gameplay_rating)::numeric(10, 2) AS avg_gameplay_rating",
            "AVG(atmosphere_rating)::numeric(10, 2) AS avg_atmosphere_rating"
        )
            .from("games.ratings")
            .toSql()

        // --- CTE 4: most_played_games ---
        // Prostsze i bardziej "postgresowe" - LIMIT bezpośrednio w CTE.
        val mostPlayedGamesQuery = dataAccess.select(
            "array_agg(ROW(g.id, g.name, pt.play_time_hours)::games.dashboard_game_by_time ORDER BY pt.play_time_hours DESC) AS games"
        )
            .from("games.play_time pt JOIN games.games g ON pt.game_id = g.id")
            .where(
                "pt.game_id IN (${
                    dataAccess.select("game_id").from("games.play_time").orderBy("play_time_hours DESC NULLS LAST")
                        .limit(5).toSql()
                })"
            )
            .toSql()

        // --- CTE 5: highest_rated_games ---
        // Złożenie w jednym CTE, co jest bardziej naturalne.
        val avgRatingCalculation = """
        (
            (COALESCE(story_rating, 0) + COALESCE(gameplay_rating, 0) + COALESCE(atmosphere_rating, 0))
            /
            NULLIF(
                (CASE WHEN story_rating IS NOT NULL THEN 1 ELSE 0 END +
                 CASE WHEN gameplay_rating IS NOT NULL THEN 1 ELSE 0 END +
                 CASE WHEN atmosphere_rating IS NOT NULL THEN 1 ELSE 0 END), 0
            )
        )::numeric(10, 2)
    """.trimIndent()

        val highestRatedGamesQuery = dataAccess.select(
            "array_agg(ROW(g.id, g.name, r.avg_rating)::games.dashboard_game_by_rating ORDER BY r.avg_rating DESC) AS games"
        )
            .from(
                "(${
                    dataAccess.select("game_id, $avgRatingCalculation AS avg_rating")
                        .from("games.ratings")
                        .where("$avgRatingCalculation IS NOT NULL") // Wyrzucamy gry bez ocen od razu
                        .orderBy("avg_rating DESC")
                        .limit(5)
                        .toSql()
                }) r JOIN games.games g ON r.game_id = g.id"
            )
            .toSql()

        // --- CTE 6: favorite_category ---
        // Bez zmian.
        val favoriteCategoryQuery = dataAccess.select("c.name")
            .from("games.categories_to_games ctg JOIN games.play_time pt ON ctg.game_id = pt.game_id JOIN games.categories c ON ctg.category_id = c.id")
            .groupBy("c.id, c.name")
            .orderBy("SUM(pt.play_time_hours) DESC")
            .limit(1)
            .toSql()

        // --- CTE 7: favorite_series ---
        // Bez zmian.
        val favoriteSeriesQuery = dataAccess.select("s.name")
            .from("games.games g JOIN games.series s ON g.series = s.id JOIN games.play_time pt ON g.id = pt.game_id")
            .groupBy("s.id, s.name")
            .orderBy("SUM(pt.play_time_hours) DESC")
            .limit(1)
            .toSql()

        return dataAccess.select(
            """
            kpi.total_games,
            kpi.total_playtime_hours,
            kpi.played_games_count,
            kpi.avg_playtime_for_played,
            sd.distribution AS status_distribution,
            rs.avg_story_rating,
            rs.avg_gameplay_rating,
            rs.avg_atmosphere_rating,
            mpg.games AS most_played_games,
            hrg.games AS highest_rated_games,
            fc.name AS favorite_category_name,
            fs.name AS favorite_series_name
            """
        )
            .with("kpi_stats", kpiStatsQuery)
            .with("status_distribution", statusDistributionQuery)
            .with("rating_stats", ratingStatsQuery)
            .with("most_played_games", mostPlayedGamesQuery)
            .with("highest_rated_games", highestRatedGamesQuery)
            .with("favorite_category", favoriteCategoryQuery)
            .with("favorite_series", favoriteSeriesQuery)
            .from("kpi_stats kpi, status_distribution sd, rating_stats rs, most_played_games mpg, highest_rated_games hrg, favorite_category fc, favorite_series fs")
            .toSql()
    }

    fun loadData() {
        _state.update { it.copy(isLoading = true) }

        val query = buildGameStatisticsQuery()
        dataAccess.rawQuery(query).async(scope).toSingleOf<GameStatisticsData> { result ->
            when (result) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(
                            data = result.value,
                            isLoading = false
                        )
                    }
                }

                is DataResult.Failure -> {
                    showError(result.error)
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun showError(error: DatabaseException) {
        GlobalDialogManager.show(ErrorDialogConfig(error))
    }
}