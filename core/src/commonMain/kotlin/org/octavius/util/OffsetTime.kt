package org.octavius.util

import kotlinx.datetime.LocalTime
import kotlinx.datetime.UtcOffset

/**
 * Reprezentuje czas z offsetem UTC, w pełni oparty na kotlinx.datetime.
 * Zastępuje `java.time.OffsetTime`.
 */
data class OffsetTime(val time: LocalTime, val offset: UtcOffset) {
    override fun toString(): String {
        return "${time.toString().take(8)}${offset}"
    }

    companion object {
        fun parse(value: String): OffsetTime {
            val time = LocalTime.parse(value.take(8))
            val offset = UtcOffset.parse(value.substring(9))
            return OffsetTime(time, offset)
        }
    }
}
