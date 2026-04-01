package org.octavius.modules.activity.statistics

import kotlinx.datetime.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toListOf

class StatisticsHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()

    fun loadCategoryBreakdown(date: LocalDate): List<PieSlice> {
        val startOfDay = date.atTime(0, 0, 0)
        val endOfDay = date.atTime(23, 59, 59)

        val result = dataAccess.select(
            "COALESCE(c.name, 'Uncategorized') as category_name",
            "COALESCE(c.color, '#9CA3AF') as color",
            "SUM(al.duration_seconds) as total_seconds"
        )
            .from("activity_tracker.activity_log al LEFT JOIN activity_tracker.categories c ON al.category_id = c.id")
            .where("al.started_at >= :start_time AND al.started_at <= :end_time AND al.ended_at IS NOT NULL")
            .groupBy("c.name, c.color")
            .orderBy("total_seconds DESC")
            .toListOf<CategoryBreakdownDto>(
                "start_time" to startOfDay.toInstant(TimeZone.currentSystemDefault()),
                "end_time" to endOfDay.toInstant(TimeZone.currentSystemDefault())
            )

        return when (result) {
            is DataResult.Success -> {
                val total = result.value.sumOf { it.totalSeconds ?: 0L }
                if (total == 0L) return emptyList()

                result.value.map { dto ->
                    PieSlice(
                        label = dto.categoryName,
                        value = dto.totalSeconds ?: 0L,
                        color = dto.color,
                        percent = (dto.totalSeconds ?: 0L).toFloat() / total
                    )
                }
            }
            is DataResult.Failure -> emptyList()
        }
    }

    fun loadTopApplications(date: LocalDate): List<TopApplication> {
        val startOfDay = date.atTime(0, 0, 0)
        val endOfDay = date.atTime(23, 59, 59)

        val result = dataAccess.select(
            "al.process_name",
            "c.name as category_name",
            "SUM(al.duration_seconds) as total_seconds"
        )
            .from("activity_tracker.activity_log al LEFT JOIN activity_tracker.categories c ON al.category_id = c.id")
            .where("al.started_at >= :start_time AND al.started_at <= :end_time AND al.ended_at IS NOT NULL")
            .groupBy("al.process_name, c.name")
            .orderBy("total_seconds DESC")
            .toListOf<TopApplicationDto>(
                "start_time" to startOfDay.toInstant(TimeZone.currentSystemDefault()),
                "end_time" to endOfDay.toInstant(TimeZone.currentSystemDefault())
            )

        return when (result) {
            is DataResult.Success -> result.value.take(10).map { dto ->
                TopApplication(
                    name = dto.processName,
                    totalSeconds = dto.totalSeconds ?: 0L,
                    categoryName = dto.categoryName
                )
            }
            is DataResult.Failure -> emptyList()
        }
    }

    fun loadTotalTime(date: LocalDate): Long {
        val startOfDay = date.atTime(0, 0, 0)
        val endOfDay = date.atTime(23, 59, 59)

        val result = dataAccess.select("COALESCE(SUM(duration_seconds), 0) as total")
            .from("activity_tracker.activity_log")
            .where("started_at >= :start_time AND started_at <= :end_time AND ended_at IS NOT NULL")
            .toListOf<TotalTimeDto>(
                "start_time" to startOfDay.toInstant(TimeZone.currentSystemDefault()),
                "end_time" to endOfDay.toInstant(TimeZone.currentSystemDefault())
            )

        return when (result) {
            is DataResult.Success -> result.value.firstOrNull()?.total ?: 0L
            is DataResult.Failure -> 0L
        }
    }
}

private data class CategoryBreakdownDto(
    val categoryName: String,
    val color: String,
    val totalSeconds: Long?
)

private data class TopApplicationDto(
    val processName: String,
    val categoryName: String?,
    val totalSeconds: Long?
)

private data class TotalTimeDto(
    val total: Long
)
