package org.octavius.modules.activity.timeline

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import io.github.octaviusframework.db.api.DataAccess
import io.github.octaviusframework.db.api.DataResult
import io.github.octaviusframework.db.api.builder.toListOf
import org.octavius.modules.activity.domain.DocumentType
import org.octavius.ui.timeline.TimelineBlock
import org.octavius.ui.timeline.TimelineLane
import org.octavius.util.ColorUtils.hexToColor
import kotlin.time.Instant

class TimelineHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()

    fun loadLanes(date: LocalDate): List<TimelineLane> {
        val tz = TimeZone.currentSystemDefault()
        val startInstant = date.atTime(0, 0, 0).toInstant(tz)
        val endInstant = date.atTime(23, 59, 59).toInstant(tz)
        val params = mapOf("start" to startInstant, "end" to endInstant)

        val catResult = dataAccess.select(
            "cs.id",
            "c.name as label",
            "cs.started_at", "cs.ended_at",
            "c.color",
        )
            .from("activity_tracker.category_slots cs JOIN activity_tracker.categories c ON cs.category_id = c.id")
            .where("cs.started_at >= @start AND cs.started_at <= @end")
            .orderBy("cs.started_at")
            .toListOf<CategorySlotDto>(params)

        val appResult = dataAccess.select(
            "al.window_title as label",
            "al.process_name as description",
            "al.started_at", "al.ended_at",
            "COALESCE(pc.color, '#6366F1') as color",
        )
            .from("""
                activity_tracker.activity_log al
                LEFT JOIN activity_tracker.process_colors pc ON al.process_name = pc.process_name
            """.trimIndent())
            .where("al.started_at >= @start AND al.started_at <= @end")
            .orderBy("al.started_at")
            .toListOf<AppBlockDto>(params)

        val docResult = dataAccess.select("d.path", "d.type", "d.timestamp")
            .from("activity_tracker.documents d")
            .where("d.timestamp >= @start AND d.timestamp <= @end")
            .toListOf<DocBlockDto>(params)

        val categories = (catResult as? DataResult.Success)?.value?.map { it.toTimelineBlock(tz) } ?: emptyList()
        val apps = (appResult as? DataResult.Success)?.value?.map { it.toTimelineBlock(tz) } ?: emptyList()
        val docs = (docResult as? DataResult.Success)?.value?.map { it.toTimelineBlock(tz) } ?: emptyList()

        return listOf(
            TimelineLane("Kategorie", categories.mergeAdjacent()),
            TimelineLane("Aplikacje", apps),
            TimelineLane("Dokumenty", docs),
        )
    }

    private fun List<TimelineBlock>.mergeAdjacent(): List<TimelineBlock> {
        if (isEmpty()) return this
        val result = mutableListOf<TimelineBlock>()
        var current = first()
        for (i in 1 until size) {
            val next = this[i]
            if (next.label == current.label && next.color == current.color && (next.startSeconds - current.endSeconds) <= 2f) {
                current = current.copy(endSeconds = next.endSeconds)
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)
        return result
    }

    private fun mergeAutoFillSlots(slots: List<AutoFillSlotDto>): List<AutoFillSlotDto> {
        if (slots.isEmpty()) return emptyList()
        val result = mutableListOf<AutoFillSlotDto>()
        var current = slots.first()
        for (i in 1 until slots.size) {
            val next = slots[i]
            if (next.categoryId == current.categoryId && next.startedAt <= (current.endedAt ?: current.startedAt)) {
                val newEnd = if (next.endedAt != null && (current.endedAt == null || next.endedAt > current.endedAt)) {
                    next.endedAt
                } else {
                    current.endedAt
                }
                current = current.copy(endedAt = newEnd)
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)
        return result
    }

    fun autoFillAllCategories(date: LocalDate) {
        val tz = TimeZone.currentSystemDefault()
        val startInstant = date.atTime(0, 0, 0).toInstant(tz)
        val endInstant = date.atTime(23, 59, 59).toInstant(tz)
        val params = mapOf("start" to startInstant, "end" to endInstant)

        dataAccess.deleteFrom("activity_tracker.category_slots")
            .where("source_process_name IS NOT NULL AND started_at >= @start AND started_at <= @end")
            .execute(params)

        val slotsResult = dataAccess.select(
            "al.started_at", "al.ended_at",
            "cr.category_id",
            "al.process_name as source_process_name",
        )
            .from("""
                activity_tracker.activity_log al
                JOIN LATERAL (
                    SELECT category_id
                    FROM activity_tracker.categorization_rules
                    WHERE pattern = al.process_name
                        AND match_type = 'PROCESS_NAME'::activity_tracker.match_type
                        AND is_active = true
                    ORDER BY priority DESC
                    LIMIT 1
                ) cr ON true
            """.trimIndent())
            .where("al.started_at >= @start AND al.started_at <= @end AND al.ended_at IS NOT NULL")
            .orderBy("al.started_at")
            .toListOf<AutoFillSlotDto>(params)

        val slots = (slotsResult as? DataResult.Success)?.value ?: return
        val mergedSlots = mergeAutoFillSlots(slots)

        for (slot in mergedSlots) {
            val ended = slot.endedAt ?: continue
            val slotValues = mapOf(
                "category_id" to slot.categoryId,
                "started_at" to slot.startedAt,
                "ended_at" to ended,
                "source_process_name" to slot.sourceProcessName,
            )
            dataAccess.insertInto("activity_tracker.category_slots")
                .values(slotValues)
                .execute(slotValues)
        }
    }

    fun autoFillCategoryForProcess(processName: String, date: LocalDate): Boolean {
        val tz = TimeZone.currentSystemDefault()
        val startInstant = date.atTime(0, 0, 0).toInstant(tz)
        val endInstant = date.atTime(23, 59, 59).toInstant(tz)

        val ruleResult = dataAccess.select("category_id")
            .from("activity_tracker.categorization_rules")
            .where("""
                match_type = 'PROCESS_NAME'::activity_tracker.match_type
                AND pattern = @pattern
                AND is_active = true
            """.trimIndent())
            .orderBy("priority DESC")
            .toListOf<CategoryRuleDto>(mapOf("pattern" to processName))

        val categoryId = (ruleResult as? DataResult.Success)?.value?.firstOrNull()?.categoryId ?: return false

        val logResult = dataAccess.select("started_at", "ended_at")
            .from("activity_tracker.activity_log")
            .where("process_name = @process AND started_at >= @start AND started_at <= @end AND ended_at IS NOT NULL")
            .orderBy("started_at")
            .toListOf<ActivityTimeDto>(mapOf("process" to processName, "start" to startInstant, "end" to endInstant))

        val logs = (logResult as? DataResult.Success)?.value ?: return false

        dataAccess.deleteFrom("activity_tracker.category_slots")
            .where("source_process_name = @process AND started_at >= @start AND started_at <= @end")
            .execute(mapOf("process" to processName, "start" to startInstant, "end" to endInstant))

        val autoFillSlots = logs.map { 
            AutoFillSlotDto(it.startedAt, it.endedAt, categoryId, processName)
        }
        val mergedSlots = mergeAutoFillSlots(autoFillSlots)

        for (slot in mergedSlots) {
            val ended = slot.endedAt ?: continue
            val slotValues = mapOf(
                "category_id" to categoryId,
                "started_at" to slot.startedAt,
                "ended_at" to ended,
                "source_process_name" to processName,
            )
            dataAccess.insertInto("activity_tracker.category_slots")
                .values(slotValues)
                .execute(slotValues)
        }

        return true
    }

    fun deleteCategorySlot(id: Long) {
        dataAccess.deleteFrom("activity_tracker.category_slots")
            .where("id = @id")
            .execute(mapOf("id" to id))
    }

    private fun CategorySlotDto.toTimelineBlock(tz: TimeZone): TimelineBlock =
        createTimelineBlock(startedAt, endedAt, color, label, id = id, tz = tz)

    private fun AppBlockDto.toTimelineBlock(tz: TimeZone): TimelineBlock =
        createTimelineBlock(startedAt, endedAt, color, label, description, tz = tz)

    private fun DocBlockDto.toTimelineBlock(tz: TimeZone): TimelineBlock {
        val t = timestamp.toLocalDateTime(tz)
        val startSec = t.hour * 3600f + t.minute * 60f + t.second
        val fileName = path.substringAfterLast("/").substringAfterLast("\\")
        return TimelineBlock(
            startSeconds = startSec,
            endSeconds = (startSec + 120f).coerceAtMost(86400f),
            color = hexToColor(colorForDocType(type)) ?: Color.Gray,
            label = fileName,
        )
    }

    private fun createTimelineBlock(
        startedAt: Instant,
        endedAt: Instant?,
        colorHex: String,
        label: String,
        description: String = "",
        id: Long? = null,
        tz: TimeZone
    ): TimelineBlock {
        val s = startedAt.toLocalDateTime(tz)
        val e = endedAt?.toLocalDateTime(tz) ?: s
        val startSec = s.hour * 3600f + s.minute * 60f + s.second
        val endSec = (e.hour * 3600f + e.minute * 60f + e.second).coerceAtLeast(startSec + 1f)
        return TimelineBlock(
            startSeconds = startSec,
            endSeconds = endSec,
            color = hexToColor(colorHex) ?: Color.Gray,
            label = label,
            description = description,
            id = id
        )
    }

    private fun colorForDocType(type: DocumentType) = when (type) {
        DocumentType.Pdf -> "#DC2626"
        DocumentType.Image, DocumentType.Video -> "#7C3AED"
        DocumentType.Code -> "#059669"
        else -> "#D97706"
    }
}

// DTOs
internal data class CategorySlotDto(
    val id: Long,
    val label: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val color: String,
)

internal data class AppBlockDto(
    val label: String,
    val description: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val color: String,
)

internal data class DocBlockDto(
    val path: String,
    val type: DocumentType,
    val timestamp: Instant,
)

internal data class CategoryRuleDto(val categoryId: Int)

internal data class ActivityTimeDto(val startedAt: Instant, val endedAt: Instant?)

internal data class AutoFillSlotDto(
    val startedAt: Instant,
    val endedAt: Instant?,
    val categoryId: Int,
    val sourceProcessName: String,
)
