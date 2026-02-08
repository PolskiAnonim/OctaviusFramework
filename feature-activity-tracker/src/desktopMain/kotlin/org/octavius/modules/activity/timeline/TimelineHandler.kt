package org.octavius.modules.activity.timeline

import kotlinx.datetime.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toListOf
import org.octavius.modules.activity.domain.DocumentType
import kotlin.time.Instant

class TimelineHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()

    // Pobiera wszystko na raz dla wydajności (lub można to rozbić)
    suspend fun loadDailyData(date: LocalDate): UnifiedTimelineState {
        val startOfDay = date.atTime(0, 0, 0)
        val endOfDay = date.atTime(23, 59, 59)
        val startInstant = startOfDay.toInstant(TimeZone.currentSystemDefault())
        val endInstant = endOfDay.toInstant(TimeZone.currentSystemDefault())
        val tz = TimeZone.currentSystemDefault()

        // 1. POBIERANIE DANYCH DO PASKA KATEGORII (Grupowanie po kolorze kategorii)
        // Używamy tego samego logu, ale kolor bierzemy z kategorii
        val catResult = dataAccess.select(
            "al.id", "al.window_title", "c.name as cat_name",
            "al.started_at", "al.ended_at",
            "COALESCE(c.color, '#333333') as color" // Fallback color
        )
            .from("activity_tracker.activity_log al LEFT JOIN activity_tracker.categories c ON al.category_id = c.id")
            .where("al.started_at >= :start AND al.started_at <= :end")
            .toListOf<RawDto>("start" to startInstant, "end" to endInstant)

        // 2. POBIERANIE DANYCH DO PASKA APLIKACJI (Kolor z process_colors)
        val appResult = dataAccess.select(
            "al.id", "al.window_title", "al.process_name",
            "al.started_at", "al.ended_at",
            "COALESCE(pc.color, '#6366F1') as color" // Fallback color
        )
            .from("""
            activity_tracker.activity_log al 
            LEFT JOIN activity_tracker.process_colors pc ON al.process_name = pc.process_name
        """.trimIndent())
            .where("al.started_at >= :start AND al.started_at <= :end")
            .toListOf<RawDto>("start" to startInstant, "end" to endInstant)

        // 3. POBIERANIE DOKUMENTÓW
        val docResult = dataAccess.select("d.id", "d.path", "d.type", "d.timestamp")
            .from("activity_tracker.documents d")
            .where("d.timestamp >= :start AND d.timestamp <= :end")
            .toListOf<DocDto>("start" to startInstant, "end" to endInstant)

        // Mapowanie wyników
        val categories = (catResult as? DataResult.Success)?.value?.map { it.toTimeBlock(tz, true) } ?: emptyList()
        val apps = (appResult as? DataResult.Success)?.value?.map { it.toTimeBlock(tz, false) } ?: emptyList()
        val docs = (docResult as? DataResult.Success)?.value?.map { it.toTimePoint(tz) } ?: emptyList()

        return UnifiedTimelineState(
            selectedDate = date,
            categoryBlocks = categories,
            appBlocks = apps,
            documentPoints = docs
        )
    }

    // Helper mapping extensions
    private fun RawDto.toTimeBlock(tz: TimeZone, isCategory: Boolean): TimeBlock {
        val s = startedAt.toLocalDateTime(tz)
        // Jeśli brak endedAt, zakładamy krótki czas lub "teraz"
        val e = endedAt?.toLocalDateTime(tz) ?: s
        return TimeBlock(
            id = id,
            startTime = s,
            endTime = e,
            title = if(isCategory) (processName ?: "Uncategorized") else windowTitle,
            subTitle = if(isCategory) null else processName,
            color = color
        )
    }

    private fun DocDto.toTimePoint(tz: TimeZone): TimePoint {
        return TimePoint(
            id = id,
            timestamp = timestamp.toLocalDateTime(tz),
            title = path.substringAfterLast("/").substringAfterLast("\\"),
            path = path,
            color = getColorForDocType(type)
        )
    }

    private fun getColorForDocType(type: DocumentType) = "#DC2626" // placeholder
}

// DTOs
data class RawDto(val id: Long, val windowTitle: String, val processName: String?, val startedAt: kotlin.time.Instant, val endedAt: kotlin.time.Instant?, val color: String)
data class DocDto(val id: Long, val path: String, val type: DocumentType, val timestamp: Instant)