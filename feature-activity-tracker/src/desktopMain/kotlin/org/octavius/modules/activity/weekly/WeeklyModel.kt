package org.octavius.modules.activity.weekly

import kotlinx.datetime.LocalDate

data class CategoryData(
    val name: String,
    val color: String,
    val seconds: Long
)

data class DayData(
    val date: LocalDate,
    val dayOfWeek: String,
    val categories: List<CategoryData>,
    val totalSeconds: Long
)

data class WeeklyState(
    val weekStartDate: LocalDate,
    val days: List<DayData> = emptyList(),
    val allCategories: List<Pair<String, String>> = emptyList(), // name to color
    val maxSeconds: Long = 0,
    val isLoading: Boolean = false
)
