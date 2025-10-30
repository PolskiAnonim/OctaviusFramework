package org.octavius.database.type

import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.octavius.data.OffsetTime
import org.postgresql.util.PGInterval
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/**
 * Definicja pojedynczego mapowania dla standardowego typu PostgreSQL.
 *
 * @param kotlinClass Odpowiednik KClass w Kotlinie.
 * @param fromResultSet Sposób wyciągnięcia wartości bezpośrednio z ResultSet (szybka ścieżka).
 * @param fromString Sposób parsowania wartości z jej tekstowej reprezentacji (wolna ścieżka).
 */
internal data class StandardTypeHandler(
    val kotlinClass: KClass<*>,
    val fromResultSet: ((ResultSet, Int) -> Any?)?,
    val fromString: (String) -> Any?
)

/**
 * Centralny rejestr i jedyne źródło prawdy dla mapowań standardowych typów PostgreSQL na typy Kotlina.
 *
 * Zastępuje rozproszone bloki `when` w `PostgresToKotlinConverter` i `ResultSetValueExtractor`,
 * zapewniając spójność i łatwość w rozszerzaniu.
 */
@OptIn(ExperimentalTime::class)
internal object StandardTypeMappingRegistry {

    private val POSTGRES_TIMETZ_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart()
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalEnd()
        .appendPattern("X")
        .toFormatter()

    // GŁÓWNA MAPA - JEDYNE ŹRÓDŁO PRAWDY
    private val mappings: Map<String, StandardTypeHandler> = mapOf(
        // Typy numeryczne całkowite
        "smallserial" to StandardTypeHandler(Short::class, { rs, i -> rs.getInt(i) }, { s -> s.toShort() }),
        "int2" to StandardTypeHandler(Short::class, { rs, i -> rs.getShort(i) }, { s -> s.toShort() }),
        "int4" to StandardTypeHandler(Int::class, { rs, i -> rs.getInt(i) }, { s -> s.toInt() }),
        "serial" to StandardTypeHandler(Int::class, { rs, i -> rs.getInt(i) }, { s -> s.toInt() }),
        "int8" to StandardTypeHandler(Long::class, { rs, i -> rs.getLong(i) }, { s -> s.toLong() }),
        "bigserial" to StandardTypeHandler(Long::class, { rs, i -> rs.getLong(i) }, { s -> s.toLong() }),

        // Typy zmiennoprzecinkowe
        "float4" to StandardTypeHandler(Float::class, { rs, i -> rs.getFloat(i) }, { s -> s.toFloat() }),
        "float8" to StandardTypeHandler(Double::class, { rs, i -> rs.getDouble(i) }, { s -> s.toDouble() }),
        "numeric" to StandardTypeHandler(BigDecimal::class, { rs, i -> rs.getBigDecimal(i) }, { s -> s.toBigDecimal() }),

        // Wartości logiczne
        "bool" to StandardTypeHandler(Boolean::class, { rs, i -> rs.getBoolean(i) }, { s -> s == "t" }),

        // Typy tekstowe
        "text" to StandardTypeHandler(String::class, null) { s -> s },
        "varchar" to StandardTypeHandler(String::class, null) { s -> s },
        "char" to StandardTypeHandler(String::class, null) { s -> s },

        // UUID
        "uuid" to StandardTypeHandler(UUID::class, { rs, i -> rs.getObject(i) as UUID }, { s -> UUID.fromString(s) }),

        // JSON
        "json" to StandardTypeHandler(JsonElement::class, null) { s -> Json.parseToJsonElement(s) },
        "jsonb" to StandardTypeHandler(JsonElement::class, null) { s -> Json.parseToJsonElement(s) },

        // Data i czas
        "date" to StandardTypeHandler(LocalDate::class, { rs, i -> rs.getDate(i).toLocalDate().toKotlinLocalDate() }, { s -> LocalDate.parse(s) }),
        "timestamp" to StandardTypeHandler(LocalDateTime::class, { rs, i -> rs.getTimestamp(i).toLocalDateTime().toKotlinLocalDateTime() }, { s -> LocalDateTime.parse(s.replace(' ', 'T')) }),
        "timestamptz" to StandardTypeHandler(Instant::class, { rs, i -> rs.getTimestamp(i).toInstant().toKotlinInstant() }, { s -> Instant.parse(s.replace(' ', 'T')) }),
        "time" to StandardTypeHandler(LocalTime::class, { rs, i -> rs.getTime(i).toLocalTime().toKotlinLocalTime() }, { s -> LocalTime.parse(s) }),
        "timetz" to StandardTypeHandler(
            OffsetTime::class,
            { rs, i ->
                val javaOffsetTime = rs.getObject(i, java.time.OffsetTime::class.java)
                OffsetTime(
                    time = javaOffsetTime.toLocalTime().toKotlinLocalTime(),
                    offset = UtcOffset(seconds = javaOffsetTime.offset.totalSeconds)
                )
            },
            { s ->
                val javaOffsetTime = java.time.OffsetTime.parse(s, POSTGRES_TIMETZ_FORMATTER)
                OffsetTime(
                    time = javaOffsetTime.toLocalTime().toKotlinLocalTime(),
                    offset = UtcOffset(seconds = javaOffsetTime.offset.totalSeconds)
                )
            }
        ),
        // Interwały
        "interval" to StandardTypeHandler(
            Duration::class,
            { rs, i ->
                val pgInterval = rs.getObject(i) as PGInterval
                // Konwersja z PGInterval na kotlin.time.Duration
                (pgInterval.days.toLong() * 24).hours +
                        pgInterval.hours.toLong().hours +
                        pgInterval.minutes.toLong().minutes +
                        pgInterval.seconds.seconds
            },
            { s ->
                val pgInterval = PGInterval(s)
                // Konwersja z PGInterval na kotlin.time.Duration
                (pgInterval.days.toLong() * 24).hours +
                        pgInterval.hours.toLong().hours +
                        pgInterval.minutes.toLong().minutes +
                        pgInterval.seconds.seconds
            }
        ),
        "bytea" to StandardTypeHandler(
            ByteArray::class,
            { rs, i -> rs.getBytes(i) },
            { s ->
                if (s.startsWith("\\x")) {
                    hexStringToByteArray(s.substring(2))
                } else {
                    throw UnsupportedOperationException("Unsupported bytea format. Only hex format (e.g. '\\xDEADBEEF') is supported.")
                }
            }
        ),
    )

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        if (len % 2 != 0) {
            throw IllegalArgumentException("Hex string must have an even number of characters")
        }
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    fun getHandler(pgTypeName: String): StandardTypeHandler? = mappings[pgTypeName]

    fun getAllTypeNames(): Set<String> = mappings.keys
}