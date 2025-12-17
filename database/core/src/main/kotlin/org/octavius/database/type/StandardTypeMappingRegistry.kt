package org.octavius.database.type

import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.octavius.data.OffsetTime
import org.octavius.data.type.PgStandardType
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
    private val mappings: Map<String, StandardTypeHandler> = buildMappings()

    private fun buildMappings(): Map<String, StandardTypeHandler> {
        val map = mutableMapOf<String, StandardTypeHandler>()

        PgStandardType.entries.forEach { pgType ->
            if (pgType.isArray) return@forEach
            val handler = when (pgType) {
                // Typy numeryczne całkowite
                PgStandardType.INT2, PgStandardType.SMALLSERIAL -> primitive(Short::class, ResultSet::getShort, String::toShort)
                PgStandardType.INT4, PgStandardType.SERIAL -> primitive(Int::class, ResultSet::getInt, String::toInt)
                PgStandardType.INT8, PgStandardType.BIGSERIAL -> primitive(Long::class, ResultSet::getLong, String::toLong)
                // Typy zmiennoprzecinkowe
                PgStandardType.FLOAT4 -> primitive(Float::class, ResultSet::getFloat, String::toFloat)
                PgStandardType.FLOAT8 -> primitive(Double::class, ResultSet::getDouble, String::toDouble)

                PgStandardType.NUMERIC -> standard(BigDecimal::class, ResultSet::getBigDecimal, String::toBigDecimal)
                // Typy tekstowe
                PgStandardType.TEXT, PgStandardType.VARCHAR, PgStandardType.CHAR -> fromStringOnly(String::class) { it }
                // Data i czas
                PgStandardType.DATE -> mapped(
                    LocalDate::class, ResultSet::getDate,
                    { it.toLocalDate().toKotlinLocalDate() },
                    { LocalDate.parse(it) }
                )

                PgStandardType.TIMESTAMP -> mapped(
                    LocalDateTime::class, ResultSet::getTimestamp,
                    { it.toLocalDateTime().toKotlinLocalDateTime() },
                    { LocalDateTime.parse(it.replace(' ', 'T')) }
                )

                PgStandardType.TIMESTAMPTZ -> mapped(
                    Instant::class, ResultSet::getTimestamp,
                    { it.toInstant().toKotlinInstant() },
                    { Instant.parse(it.replace(' ', 'T')) }
                )

                PgStandardType.TIME -> mapped(
                    LocalTime::class, ResultSet::getTime,
                    { it.toLocalTime().toKotlinLocalTime() },
                    { LocalTime.parse(it) }
                )

                PgStandardType.TIMETZ -> mapped(
                    OffsetTime::class, { getObject(it, java.time.OffsetTime::class.java) },
                    { javaTime ->
                        OffsetTime(
                            time = javaTime.toLocalTime().toKotlinLocalTime(),
                            offset = UtcOffset(seconds = javaTime.offset.totalSeconds)
                        )
                    },
                    { s ->
                        val javaOffsetTime = java.time.OffsetTime.parse(s, POSTGRES_TIMETZ_FORMATTER)
                        OffsetTime(
                            time = javaOffsetTime.toLocalTime().toKotlinLocalTime(),
                            offset = UtcOffset(seconds = javaOffsetTime.offset.totalSeconds)
                        )
                    }
                )

                PgStandardType.INTERVAL -> mapped(
                    Duration::class,
                    { getObject(it) as? PGInterval },
                    ::pgIntervalToDuration,
                    { s ->
                        // Konwersja z PGInterval na kotlin.time.Duration
                        pgIntervalToDuration(PGInterval(s))
                    }
                )
                // Json
                PgStandardType.JSON, PgStandardType.JSONB -> fromStringOnly(JsonElement::class) { Json.parseToJsonElement(it) }
                // Inne
                PgStandardType.BOOL -> primitive(Boolean::class, ResultSet::getBoolean) { it == "t" }

                PgStandardType.UUID -> standard(UUID::class, { getObject(it) as UUID? }, UUID::fromString)

                PgStandardType.BYTEA -> standard(ByteArray::class, ResultSet::getBytes) {
                    if (it.startsWith("\\x")) {
                        hexStringToByteArray(it.substring(2))
                    } else {
                        throw UnsupportedOperationException("Unsupported bytea format. Only hex format (e.g. '\\xDEADBEEF') is supported.")
                    }
                }
                else -> null
            }
            if (handler != null) {
                map[pgType.typeName] = handler
            }
        }
        return map.toMap()
    }

    private fun pgIntervalToDuration(pgInterval: PGInterval): Duration {
        return (pgInterval.days.toLong() * 24).hours +
                pgInterval.hours.toLong().hours +
                pgInterval.minutes.toLong().minutes +
                pgInterval.seconds.seconds
    }

    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        require(len % 2 == 0) { "Hex string must have an even number of characters" }
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

    // Śmietnik JDBC:
    // 1. Dla typów prymitywnych (int, bool, double).
    // Wywołuje getter, a potem sprawdza wasNull().
    private inline fun <reified T : Any> primitive(
        kClass: KClass<T>,
        crossinline getter: ResultSet.(Int) -> T,
        noinline parser: (String) -> T
    ) = StandardTypeHandler(
        kotlinClass = kClass,
        fromResultSet = { rs, i ->
            val v = rs.getter(i)
            if (rs.wasNull()) null else v
        },
        fromString = parser
    )
    // 3. Dla typów standardowych (bez konwersji zwracających nulle).
    private inline fun <reified T : Any> standard(
        kClass: KClass<T>,
        crossinline getter: ResultSet.(Int) -> T?,
        noinline parser: (String) -> T
    ) = StandardTypeHandler(
        kotlinClass = kClass,
        fromResultSet = { rs, i -> rs.getter(i) },
        fromString = parser
    )

    // 3. Dla typów wymagających konwersji (np. Timestamp -> Kotlin Instant).
    // Zabezpiecza przed NullPointerException w mapperze.
    private inline fun <SRC : Any, reified T : Any> mapped(
        kClass: KClass<T>,
        crossinline getter: ResultSet.(Int) -> SRC?, // Getter JDBC
        crossinline mapper: (SRC) -> T,              // Konwersja obiektu
        noinline parser: (String) -> T               // Konwersja Stringa
    ) = StandardTypeHandler(
        kotlinClass = kClass,
        fromResultSet = { rs, i -> rs.getter(i)?.let(mapper) }, // Safe call (?.) załatwia sprawę
        fromString = parser
    )
    // 4. Dla typów nie posiadającej szybszej ścieżki niż odczyt Stringa
    private inline fun <reified T : Any> fromStringOnly(
        kClass: KClass<T>,
        noinline parser: (String) -> T
    ) = StandardTypeHandler(
        kotlinClass = kClass,
        fromResultSet = null,
        fromString = parser
    )
}
