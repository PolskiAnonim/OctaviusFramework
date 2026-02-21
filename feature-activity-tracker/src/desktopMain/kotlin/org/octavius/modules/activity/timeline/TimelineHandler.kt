package org.octavius.modules.activity.timeline

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toListOf
import org.octavius.modules.activity.domain.DocumentType
import org.octavius.ui.timeline.TimelineBlock
import org.octavius.ui.timeline.TimelineLane
import kotlin.time.Instant

class TimelineHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()

    suspend fun loadLanes(date: LocalDate): List<TimelineLane> {
        val tz = TimeZone.currentSystemDefault()
        val startInstant = date.atTime(0, 0, 0).toInstant(tz)
        val endInstant = date.atTime(23, 59, 59).toInstant(tz)
        val params = mapOf("start" to startInstant, "end" to endInstant)

        val catResult = dataAccess.select(
            "COALESCE(c.name, '') as label",
            "al.started_at", "al.ended_at",
            "COALESCE(c.color, '#333333') as color"
        )
            .from("activity_tracker.activity_log al LEFT JOIN activity_tracker.categories c ON al.category_id = c.id")
            .where("al.started_at >= :start AND al.started_at <= :end")
            .toListOf<ActivityLogBlockDto>(params)

        val appResult = dataAccess.select(
            "COALESCE(al.process_name, '') as label",
            "al.started_at", "al.ended_at",
            "COALESCE(pc.color, '#6366F1') as color"
        )
            .from("""
                activity_tracker.activity_log al
                LEFT JOIN activity_tracker.process_colors pc ON al.process_name = pc.process_name
            """.trimIndent())
            .where("al.started_at >= :start AND al.started_at <= :end")
            .toListOf<ActivityLogBlockDto>(params)

        val docResult = dataAccess.select("d.path", "d.type", "d.timestamp")
            .from("activity_tracker.documents d")
            .where("d.timestamp >= :start AND d.timestamp <= :end")
            .toListOf<DocBlockDto>(params)

        val categories = (catResult as? DataResult.Success)?.value?.map { it.toTimelineBlock(tz) } ?: emptyList()
        val apps = (appResult as? DataResult.Success)?.value?.map { it.toTimelineBlock(tz) } ?: emptyList()
        val docs = (docResult as? DataResult.Success)?.value?.map { it.toTimelineBlock(tz) } ?: emptyList()

        return listOf(
            TimelineLane("Kategorie", categories),
            TimelineLane("Aplikacje", apps),
            TimelineLane("Dokumenty", docs),
        )
    }

    private fun ActivityLogBlockDto.toTimelineBlock(tz: TimeZone): TimelineBlock {
        val s = startedAt.toLocalDateTime(tz)
        val e = endedAt?.toLocalDateTime(tz) ?: s
        val startSec = s.hour * 3600f + s.minute * 60f + s.second
        val endSec = (e.hour * 3600f + e.minute * 60f + e.second).coerceAtLeast(startSec + 1f)
        return TimelineBlock(startSec, endSec, parseColor(color), label)
    }

    private fun DocBlockDto.toTimelineBlock(tz: TimeZone): TimelineBlock {
        val t = timestamp.toLocalDateTime(tz)
        val startSec = t.hour * 3600f + t.minute * 60f + t.second
        val fileName = path.substringAfterLast("/").substringAfterLast("\\")
        return TimelineBlock(
            startSeconds = startSec,
            endSeconds = (startSec + 120f).coerceAtMost(86400f),
            color = parseColor(colorForDocType(type)),
            label = fileName,
        )
    }

    private fun colorForDocType(type: DocumentType) = when (type) {
        DocumentType.Pdf -> "#DC2626"
        DocumentType.Image, DocumentType.Video -> "#7C3AED"
        DocumentType.Code -> "#059669"
        else -> "#D97706"
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        Color(when (clean.length) {
            6 -> clean.toLong(16).toInt() or 0xFF000000.toInt()
            8 -> clean.toLong(16).toInt()
            else -> 0xFF6366F1.toInt()
        })
    } catch (e: Exception) {
        Color.Gray
    }
}

data class ActivityLogBlockDto(
    val label: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val color: String,
)

data class DocBlockDto(
    val path: String,
    val type: DocumentType,
    val timestamp: Instant,
)
