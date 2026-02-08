package org.octavius.modules.activity.timeline

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class TimelineEntry(
    val id: Long,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val title: String,
    val processName: String,
    val categoryColor: String?
)

data class DocumentTimelineEntry(
    val id: Long,
    val timestamp: LocalDateTime,
    val path: String,
    val title: String?,
    val type: String,
    val color: String
)

data class TimelineState(
    val entries: List<TimelineEntry> = emptyList(),
    val selectedDate: LocalDate,
    val zoomLevel: Float = 60f,  // pixels per hour
    val selectionRange: Pair<Float, Float>? = null,
    val hoveredEntry: TimelineEntry? = null,
    val isLoading: Boolean = false
)

data class DocumentTimelineState(
    val entries: List<DocumentTimelineEntry> = emptyList(),
    val selectedDate: LocalDate,
    val zoomLevel: Float = 60f,
    val isLoading: Boolean = false
)
