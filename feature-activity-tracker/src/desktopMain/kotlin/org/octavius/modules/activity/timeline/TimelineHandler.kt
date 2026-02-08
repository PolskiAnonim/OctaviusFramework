package org.octavius.modules.activity.timeline

import kotlinx.datetime.*
import kotlin.time.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toListOf
import org.octavius.modules.activity.domain.DocumentType

class TimelineHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()

    fun loadActivities(date: LocalDate): List<TimelineEntry> {
        val startOfDay = date.atTime(0, 0, 0)
        val endOfDay = date.atTime(23, 59, 59)

        val result = dataAccess.select(
            "al.id",
            "al.window_title",
            "al.process_name",
            "al.started_at",
            "al.ended_at",
            "c.color as category_color"
        )
            .from("activity_tracker.activity_log al LEFT JOIN activity_tracker.categories c ON al.category_id = c.id")
            .where("al.started_at >= :start_time AND al.started_at <= :end_time")
            .orderBy("al.started_at")
            .toListOf<TimelineEntryDto>(
                "start_time" to startOfDay.toInstant(TimeZone.currentSystemDefault()),
                "end_time" to endOfDay.toInstant(TimeZone.currentSystemDefault())
            )

        return when (result) {
            is DataResult.Success -> result.value.map { dto ->
                TimelineEntry(
                    id = dto.id,
                    startTime = dto.startedAt.toLocalDateTime(TimeZone.currentSystemDefault()),
                    endTime = dto.endedAt?.toLocalDateTime(TimeZone.currentSystemDefault()),
                    title = dto.windowTitle,
                    processName = dto.processName,
                    categoryColor = dto.categoryColor
                )
            }
            is DataResult.Failure -> emptyList()
        }
    }

    fun loadDocuments(date: LocalDate): List<DocumentTimelineEntry> {
        val startOfDay = date.atTime(0, 0, 0)
        val endOfDay = date.atTime(23, 59, 59)

        val result = dataAccess.select(
            "d.id",
            "d.path",
            "d.title",
            "d.type",
            "d.timestamp"
        )
            .from("activity_tracker.documents d")
            .where("d.timestamp >= :start_time AND d.timestamp <= :end_time")
            .orderBy("d.timestamp")
            .toListOf<DocumentTimelineDto>(
                "start_time" to startOfDay.toInstant(TimeZone.currentSystemDefault()),
                "end_time" to endOfDay.toInstant(TimeZone.currentSystemDefault())
            )

        return when (result) {
            is DataResult.Success -> result.value.map { dto ->
                DocumentTimelineEntry(
                    id = dto.id,
                    timestamp = dto.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()),
                    path = dto.path,
                    title = dto.title,
                    type = dto.type.name,
                    color = getColorForDocumentType(dto.type)
                )
            }
            is DataResult.Failure -> emptyList()
        }
    }

    private fun getColorForDocumentType(type: DocumentType): String {
        return when (type) {
            DocumentType.Pdf -> "#DC2626"
           DocumentType.Word -> "#2563EB"
            DocumentType.Excel -> "#16A34A"
            DocumentType.Powerpoint -> "#EA580C"
            DocumentType.Text  -> "#6B7280"
            DocumentType.Code -> "#7C3AED"
            DocumentType.Image -> "#EC4899"
            DocumentType.Video -> "#8B5CF6"
            else -> "#9CA3AF"
        }
    }
}

data class TimelineEntryDto(
    val id: Long,
    val windowTitle: String,
    val processName: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val categoryColor: String?
)

data class DocumentTimelineDto(
    val id: Long,
    val path: String,
    val title: String?,
    val type: DocumentType,
    val timestamp: Instant
)
