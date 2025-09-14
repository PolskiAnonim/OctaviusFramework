package org.octavius.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Reprezentuje czas z offsetem UTC, w pełni oparty na kotlinx.datetime.
 * Zastępuje `java.time.OffsetTime`.
 */
data class KotlinOffsetTime(val time: LocalTime, val offset: UtcOffset) {
    override fun toString(): String {
        return "${time.toString().take(8)}${offset}"
    }

    companion object {
        fun parse(value: String): KotlinOffsetTime {
            val time = LocalTime.parse(value.take(8))
            val offset = UtcOffset.parse(value.substring(9))
            return KotlinOffsetTime(time, offset)
        }
    }
}

/**
 * Reprezentuje datę i czas z offsetem UTC, w pełni oparty na kotlinx.datetime.
 * Reprezentuje konkretny punkt w czasie, ale z zachowaniem informacji o lokalnym czasie i offsecie.
 * Idealny do reprezentowania `Instant` w UI.
 */
@OptIn(ExperimentalTime::class)
data class KotlinOffsetDateTime(val dateTime: LocalDateTime, val offset: UtcOffset) {

    fun toInstant(): Instant {
        return dateTime.toInstant(offset)
    }

    override fun toString(): String {
        return "${dateTime}${offset}"
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        fun fromInstant(instant: Instant, zone: TimeZone): KotlinOffsetDateTime {
            val local = instant.toLocalDateTime(zone)
            val offset = zone.offsetAt(instant)
            return KotlinOffsetDateTime(local, offset)
        }
    }
}