package org.octavius.modules.activity.statistics

import kotlinx.datetime.LocalDate

data class PieSlice(
    val label: String,
    val value: Long,
    val color: String,
    val percent: Float
)

data class TopApplication(
    val name: String,
    val totalSeconds: Long,
    val categoryName: String?
)

data class StatisticsState(
    val selectedDate: LocalDate,
    val slices: List<PieSlice> = emptyList(),
    val topApplications: List<TopApplication> = emptyList(),
    val totalSeconds: Long = 0,
    val isLoading: Boolean = false
)
