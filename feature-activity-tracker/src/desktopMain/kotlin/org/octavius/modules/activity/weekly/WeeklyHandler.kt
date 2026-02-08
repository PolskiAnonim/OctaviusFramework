package org.octavius.modules.activity.weekly

import kotlinx.datetime.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toListOf

class WeeklyHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()

    fun loadWeekData(weekStartDate: LocalDate): List<DayData> {
        val daysOfWeek = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd")
        val result = mutableListOf<DayData>()

        for (i in 0..6) {
            val date = weekStartDate.plus(DatePeriod(days = i))
            val startOfDay = date.atTime(0, 0, 0)
            val endOfDay = date.atTime(23, 59, 59)

            val queryResult = dataAccess.select(
                "COALESCE(c.name, 'Uncategorized') as category_name",
                "COALESCE(c.color, '#9CA3AF') as color",
                "SUM(al.duration_seconds) as total_seconds"
            )
                .from("activity_tracker.activity_log al LEFT JOIN activity_tracker.categories c ON al.category_id = c.id")
                .where("al.started_at >= :start_time AND al.started_at <= :end_time AND al.ended_at IS NOT NULL")
                .groupBy("c.name, c.color")
                .orderBy("total_seconds DESC")
                .toListOf<DayCategoryDto>(
                    "start_time" to startOfDay.toInstant(TimeZone.currentSystemDefault()),
                    "end_time" to endOfDay.toInstant(TimeZone.currentSystemDefault())
                )

            val categories = when (queryResult) {
                is DataResult.Success -> queryResult.value.map { dto ->
                    CategoryData(
                        name = dto.categoryName,
                        color = dto.color,
                        seconds = dto.totalSeconds ?: 0L
                    )
                }
                is DataResult.Failure -> emptyList()
            }

            result.add(
                DayData(
                    date = date,
                    dayOfWeek = daysOfWeek[i],
                    categories = categories,
                    totalSeconds = categories.sumOf { it.seconds }
                )
            )
        }

        return result
    }

    fun getAllCategories(days: List<DayData>): List<Pair<String, String>> {
        val categoryMap = mutableMapOf<String, String>()
        days.forEach { day ->
            day.categories.forEach { cat ->
                if (!categoryMap.containsKey(cat.name)) {
                    categoryMap[cat.name] = cat.color
                }
            }
        }
        return categoryMap.toList()
    }

    fun getWeekStart(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.ordinal // Monday = 0
        return date.minus(DatePeriod(days = dayOfWeek))
    }
}

private data class DayCategoryDto(
    val categoryName: String,
    val color: String,
    val totalSeconds: Long?
)
