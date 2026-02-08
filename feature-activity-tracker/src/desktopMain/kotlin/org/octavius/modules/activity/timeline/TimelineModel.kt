package org.octavius.modules.activity.timeline

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

// Wspólny interfejs dla wpisów, które mają czas trwania
data class TimeBlock(
    val id: Long,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val title: String,
    val subTitle: String?,
    val color: String
)

// Wpis punktowy (dokumenty)
data class TimePoint(
    val id: Long,
    val timestamp: LocalDateTime,
    val title: String,
    val path: String,
    val color: String
)

data class UnifiedTimelineState(
    val selectedDate: LocalDate,
    val categoryBlocks: List<TimeBlock> = emptyList(), // 1. Wiersz: Kategorie
    val appBlocks: List<TimeBlock> = emptyList(),      // 2. Wiersz: Aplikacje
    val documentPoints: List<TimePoint> = emptyList(), // 3. Wiersz: Dokumenty
    val zoomLevel: Float = 100f, // pixels per hour
    val isLoading: Boolean = false,
    val hoveredInfo: String? = null
)