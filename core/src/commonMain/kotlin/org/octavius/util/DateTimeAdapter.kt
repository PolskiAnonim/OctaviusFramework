package org.octavius.util

import kotlinx.datetime.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class DateTimeComponent { DATE, TIME, SECONDS, OFFSET }

data class DateTimePickerState(
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val seconds: Int? = null,
    val offset: UtcOffset? = null
)

interface DateTimeAdapter<T : Any> {
    val requiredComponents: Set<DateTimeComponent>
    fun format(value: T?): String
    fun getComponents(value: T?): DateTimePickerState
    fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: UtcOffset?): T?

    fun serialize(value: T): String
    fun deserialize(value: String): T?
}

// --- Konkretne Implementacje ---
@OptIn(ExperimentalTime::class)
object DateAdapter : DateTimeAdapter<LocalDate> {
    override val requiredComponents = setOf(DateTimeComponent.DATE)
    override fun format(value: LocalDate?) = value?.toString() ?: ""
    override fun getComponents(value: LocalDate?) = DateTimePickerState(date = value)
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: UtcOffset?) = date

    override fun deserialize(value: String): LocalDate = LocalDate.parse(value)
    override fun serialize(value: LocalDate) = value.toString()
}

object LocalTimeAdapter : DateTimeAdapter<LocalTime> {
    override val requiredComponents = setOf(DateTimeComponent.TIME, DateTimeComponent.SECONDS)
    override fun format(value: LocalTime?) = value?.toString() ?: ""
    override fun getComponents(value: LocalTime?) = DateTimePickerState(
        time = value?.let { LocalTime(it.hour, it.minute) },
        seconds = value?.second
    )
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: UtcOffset?): LocalTime? {
        return time?.let { LocalTime(it.hour, it.minute, it.second) }
    }

    override fun deserialize(value: String): LocalTime = LocalTime.parse(value)
    override fun serialize(value: LocalTime) = value.toString()
}

@OptIn(ExperimentalTime::class)
object LocalDateTimeAdapter : DateTimeAdapter<LocalDateTime> {
    override val requiredComponents = setOf(DateTimeComponent.DATE, DateTimeComponent.TIME, DateTimeComponent.SECONDS)
    override fun format(value: LocalDateTime?) = value?.toString()?.replace("T", " ") ?: ""
    override fun getComponents(value: LocalDateTime?) = DateTimePickerState(
        date = value?.date,
        time = value?.let { LocalTime(it.hour, it.minute) },
        seconds = value?.second
    )
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: UtcOffset?): LocalDateTime? {
        return if (date != null && time != null) LocalDateTime(
            date,
            LocalTime(time.hour, time.minute, time.second)
        ) else null
    }

    override fun deserialize(value: String): LocalDateTime = LocalDateTime.parse(value)
    override fun serialize(value: LocalDateTime) = value.toString()
}

/**
 * Adapter dla Instant, który jest świadomy strefy czasowej.
 * UI pokaże datę, czas i offset, co daje użytkownikowi pełen kontekst.
 */
@OptIn(ExperimentalTime::class)
class InstantAdapter(private val timeZone: TimeZone = TimeZone.currentSystemDefault()) : DateTimeAdapter<Instant> {
    override val requiredComponents = setOf(DateTimeComponent.DATE, DateTimeComponent.TIME, DateTimeComponent.SECONDS)
    override fun format(value: Instant?): String {
        return value?.let {
            val offset = timeZone.offsetAt(it)
            val local = it.toLocalDateTime(timeZone)
            "${local.toString().replace('T', ' ')}$offset"
        } ?: ""
    }
    override fun getComponents(value: Instant?): DateTimePickerState {
        return value?.let {
            val offset = timeZone.offsetAt(it)
            val local = it.toLocalDateTime(timeZone)
            DateTimePickerState(
                date = local.date,
                time = local.time,
                seconds = local.second,
                offset = offset
            )
        } ?: DateTimePickerState()
    }
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: UtcOffset?): Instant? {
        return if (date != null && time != null && offset != null) {
            LocalDateTime(date, time).toInstant(offset)
        } else null
    }

    override fun deserialize(value: String): Instant = Instant.parse(value)

    override fun serialize(value: Instant) = value.toString()
}


@OptIn(ExperimentalTime::class)
object OffsetTimeAdapter : DateTimeAdapter<OffsetTime> {
    override val requiredComponents = setOf(DateTimeComponent.TIME, DateTimeComponent.SECONDS, DateTimeComponent.OFFSET)
    override fun format(value: OffsetTime?) = value?.toString() ?: ""
    override fun getComponents(value: OffsetTime?) = DateTimePickerState(
        time = value?.time,
        seconds = value?.time?.second,
        offset = value?.offset
    )
    override fun buildFromComponents(date: LocalDate?, time: LocalTime?, offset: UtcOffset?): OffsetTime? {
        return if (time != null && offset != null) OffsetTime(time, offset) else null
    }

    override fun deserialize(value: String): OffsetTime = OffsetTime.parse(value)
    override fun serialize(value: OffsetTime) = value.toString()
}