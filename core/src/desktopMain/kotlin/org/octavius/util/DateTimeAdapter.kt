package org.octavius.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime
import kotlinx.datetime.toLocalDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class DateTimeComponent { DATE, TIME, SECONDS, OFFSET }

data class DateTimePickerState(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val seconds: Int? = null,
    val offset: ZoneOffset? = null
)

interface DateTimeAdapter<T : Any> {
    val requiredComponents: Set<DateTimeComponent>
    fun format(value: T?): String
    fun getEpochMillis(value: T?): Long?
    fun dateFromEpochMillis(millis: Long): LocalDate
    fun getComponents(value: T?): DateTimePickerState
    fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: ZoneOffset?): T?
}

// --- Konkretne Implementacje ---
@OptIn(ExperimentalTime::class)
object DateAdapter : DateTimeAdapter<LocalDate> {
    override val requiredComponents = setOf(DateTimeComponent.DATE)
    override fun format(value: LocalDate?) = value?.toString() ?: ""
    override fun getEpochMillis(value: LocalDate?) = value?.atStartOfDayIn(TimeZone.Companion.UTC)?.toEpochMilliseconds()
    override fun dateFromEpochMillis(millis: Long) = Instant.Companion.fromEpochMilliseconds(millis).toLocalDateTime(
        TimeZone.Companion.UTC).date
    override fun getComponents(value: LocalDate?) = DateTimePickerState(date = value)
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: ZoneOffset?) = date
}

object LocalTimeAdapter : DateTimeAdapter<LocalTime> {
    override val requiredComponents = setOf(DateTimeComponent.TIME, DateTimeComponent.SECONDS)
    override fun format(value: LocalTime?) = value?.toString() ?: ""
    override fun getEpochMillis(value: LocalTime?) = null
    override fun dateFromEpochMillis(millis: Long): LocalDate = throw UnsupportedOperationException()
    override fun getComponents(value: LocalTime?) = DateTimePickerState(
        time = value?.let { LocalTime(it.hour, it.minute, it.second) },
        seconds = value?.second
    )
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: ZoneOffset?): LocalTime? {
        return time?.let { LocalTime(it.hour, it.minute, it.second) }
    }
}

@OptIn(ExperimentalTime::class)
object TimestampAdapter : DateTimeAdapter<LocalDateTime> {
    override val requiredComponents = setOf(DateTimeComponent.DATE, DateTimeComponent.TIME, DateTimeComponent.SECONDS)
    override fun format(value: LocalDateTime?) = value?.toString()?.replace("T", " ") ?: ""
    override fun getEpochMillis(value: LocalDateTime?) = value?.toInstant(TimeZone.Companion.UTC)?.toEpochMilliseconds()
    override fun dateFromEpochMillis(millis: Long) = Instant.Companion.fromEpochMilliseconds(millis).toLocalDateTime(
        TimeZone.Companion.UTC).date
    override fun getComponents(value: LocalDateTime?) = DateTimePickerState(
        date = value?.date,
        time = value?.let { LocalTime(it.hour, it.minute, it.second) },
        seconds = value?.second
    )
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: ZoneOffset?): LocalDateTime? {
        return if (date != null && time != null) LocalDateTime(
            date,
            LocalTime(time.hour, time.minute, time.second)
        ) else null
    }
}

@OptIn(ExperimentalTime::class)
class InstantAdapter(private val timeZone: TimeZone = TimeZone.Companion.currentSystemDefault()) : DateTimeAdapter<Instant> {
    override val requiredComponents = setOf(DateTimeComponent.DATE, DateTimeComponent.TIME, DateTimeComponent.SECONDS)
    override fun format(value: Instant?) = value?.toLocalDateTime(timeZone)?.toString()?.replace("T", " ") ?: ""
    override fun getEpochMillis(value: Instant?) = value?.toEpochMilliseconds()
    override fun dateFromEpochMillis(millis: Long) = Instant.Companion.fromEpochMilliseconds(millis).toLocalDateTime(
        TimeZone.Companion.UTC).date
    override fun getComponents(value: Instant?): DateTimePickerState {
        val local = value?.toLocalDateTime(timeZone)
        return DateTimePickerState(
            date = local?.date,
            time = local?.let { LocalTime(it.hour, it.minute, it.second) },
            seconds = local?.second
        )
    }
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: ZoneOffset?): Instant? {
        return if (date != null && time != null) {
            LocalDateTime(date, LocalTime(time.hour, time.minute, time.second)).toInstant(timeZone)
        } else null
    }
}

@OptIn(ExperimentalTime::class)
object OffsetTimeAdapter : DateTimeAdapter<OffsetTime> {
    override val requiredComponents = setOf(DateTimeComponent.TIME, DateTimeComponent.SECONDS, DateTimeComponent.OFFSET)
    override fun format(value: OffsetTime?) = value?.toString() ?: ""
    override fun getEpochMillis(value: OffsetTime?) = null
    override fun dateFromEpochMillis(millis: Long): LocalDate = throw UnsupportedOperationException()
    override fun getComponents(value: OffsetTime?) = DateTimePickerState(
        time = value?.toLocalTime()?.toKotlinLocalTime(),
        seconds = value?.second,
        offset = value?.offset
    )
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: ZoneOffset?): OffsetTime? {
        return if (time != null && offset != null) OffsetTime.of(time.toJavaLocalTime(), offset) else null
    }
}