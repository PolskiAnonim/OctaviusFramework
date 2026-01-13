package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonElement
import org.octavius.data.OffsetTime
import org.octavius.data.exception.ConversionException
import org.octavius.data.exception.ConversionExceptionMessage
import org.octavius.data.exception.TypeRegistryException
import org.octavius.data.exception.TypeRegistryExceptionMessage
import org.octavius.data.toMap
import org.octavius.data.type.DynamicDto
import org.octavius.data.type.PgTyped
import org.octavius.data.util.clean
import org.octavius.database.config.DynamicDtoSerializationStrategy
import org.octavius.database.type.registry.TypeRegistry
import org.postgresql.util.PGobject
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/**
 * Wynik ekspansji zapytania do formatu pozycjonowanego (JDBC).
 * @param sql Zapytanie z placeholderami '?' zamiast nazwanych parametrów.
 * @param params Uporządkowana lista parametrów do użycia w `PreparedStatement`.
 */
data class PositionalQuery(
    val sql: String,
    val params: List<Any?>
)

/**
 * Konwertuje złożone typy Kotlin na odpowiednie konstrukcje SQL dla PostgreSQL.
 *
 * Umożliwia używanie w zapytaniach zaawansowanych typów bez manualnej konwersji.
 * Obsługiwane transformacje:
 * - `List<T>` -> `ARRAY[...]`
 * - `data class` -> `ROW(...)::type_name`
 * - `Enum` -> `PGobject` (z mapowaniem `CamelCase` na `snake_case`)
 * - `JsonObject` -> `JSONB`
 * - `PgTyped` -> jak wyżej oraz dodaje rzutowanie `::type_name` - należy uważać na data class
 */
internal class KotlinToPostgresConverter(
    private val typeRegistry: TypeRegistry,
    private val dynamicDtoStrategy: DynamicDtoSerializationStrategy = DynamicDtoSerializationStrategy.AUTOMATIC_WHEN_UNAMBIGUOUS
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @OptIn(ExperimentalTime::class)
    private val KOTLIN_TO_JDBC_CONVERTERS: Map<KClass<*>, (Any) -> Any> = mapOf(
        LocalDate::class to { v -> java.sql.Date.valueOf((v as LocalDate).toJavaLocalDate()) },
        LocalDateTime::class to { v -> java.sql.Timestamp.valueOf((v as LocalDateTime).toJavaLocalDateTime()) },
        LocalTime::class to { v -> java.sql.Time.valueOf((v as LocalTime).toJavaLocalTime()) },
        Instant::class to { v -> java.sql.Timestamp.from((v as Instant).toJavaInstant()) },
        OffsetTime::class to { v ->
            val ktTime = v as OffsetTime
            val javaOffset = ZoneOffset.ofTotalSeconds(ktTime.offset.totalSeconds)
            val javaTime = ktTime.time.toJavaLocalTime()
            java.time.OffsetTime.of(javaTime, javaOffset)
        },
        Duration::class to { v ->
            PGobject().apply {
                type = "interval"
                value = (v as Duration).toIsoString()
            }
        }
    )


    /**
     * Przetwarza zapytanie SQL, rozszerzając parametry złożone na konstrukcje PostgreSQL.
     *
     * Obsługuje:
     * - List<T> -> ARRAY[...]
     * - data class -> ROW(...)::type_name
     * - Enum -> PGobject z odpowiednią konwencją nazw
     * - JsonObject -> JSONB
     * - PgTyped -> dodaje rzutowanie ::type_name
     * - Typy daty/czasu -> odpowiednie typy java.sql
     *
     * @param sql Zapytanie z nazwanymi parametrami (np. `:param`).
     * @param params Mapa parametrów do ekspansji, może zawierać złożone typy Kotlin.
     * @return `ExpandedQuery` z przetworzonym SQL i spłaszczonymi parametrami.
     */
    fun expandParametersInQuery(sql: String, params: Map<String, Any?>): PositionalQuery {
        logger.debug { "Expanding parameters to positional query. Original params count: ${params.size}" }
        logger.trace { "Original SQL: $sql" }

        val parsedParameters = PostgresqlNamedParameterParser.parse(sql)

        if (parsedParameters.isEmpty()) {
            logger.debug { "No named parameters found, returning original query." }
            // Zwracamy pustą listę parametrów, bo żadne nie zostały użyte
            return PositionalQuery(sql, emptyList())
        }

        val expandedSqlBuilder = StringBuilder(sql.length)
        val expandedParamsList = mutableListOf<Any?>() // ZMIANA: Zamiast mapy, mamy listę
        var lastIndex = 0

        parsedParameters.forEach { parsedParam ->
            val paramName = parsedParam.name

            if (!params.containsKey(paramName)) {
                throw IllegalArgumentException("Missing value for required SQL parameter: $paramName")
            }

            val paramValue = params[paramName]

            // ZMIANA: expandParameter zwraca teraz listę wartości
            val (newPlaceholder, newParams) = expandParameter(paramValue)

            expandedSqlBuilder.append(sql, lastIndex, parsedParam.startIndex)
            expandedSqlBuilder.append(newPlaceholder)

            expandedParamsList.addAll(newParams) // ZMIANA: Dodajemy do listy

            lastIndex = parsedParam.endIndex
        }

        expandedSqlBuilder.append(sql, lastIndex, sql.length)

        logger.debug { "Parameter expansion completed. Positional params count: ${expandedParamsList.size}" }
        logger.trace { "Expanded SQL: $expandedSqlBuilder" }

        return PositionalQuery(expandedSqlBuilder.toString(), expandedParamsList)
    }

    /**
     * Rozszerza pojedynczy parametr na odpowiednią konstrukcję SQL.
     * @param paramValue Wartość parametru do konwersji.
     * @return Para: placeholder SQL (z `?`) i lista spłaszczonych parametrów.
     */
    private fun expandParameter(
        paramValue: Any?,
        appendTypeCast: Boolean = true
    ): Pair<String, List<Any?>> {
        if (paramValue == null) {
            return "?" to listOf(null) // ZMIANA: Prosty placeholder i lista z nullem
        }

        if (paramValue is PgTyped) {
            val (innerPlaceholder, innerParams) = expandParameter(
                paramValue.value,
                appendTypeCast = false
            )
            val finalPlaceholder = innerPlaceholder + "::" + paramValue.pgType
            return finalPlaceholder to innerParams
        }

        KOTLIN_TO_JDBC_CONVERTERS[paramValue::class]?.let { converter ->
            return "?" to listOf(converter(paramValue)) // ZMIANA
        }

        return when {
            paramValue is JsonElement -> {
                val pgObject = PGobject().apply {
                    type = "jsonb"
                    value = paramValue.toString()
                }
                "?" to listOf(pgObject) // ZMIANA
            }
            isDataClass(paramValue) -> {
                if (appendTypeCast) {
                    tryExpandAsDynamicDto(paramValue) ?: expandRowParameter(paramValue, true)
                } else {
                    expandRowParameter(paramValue, false)
                }
            }
            paramValue is Array<*> -> validateTypedArrayParameter(paramValue)
            paramValue is List<*> -> expandArrayParameter(paramValue)
            paramValue is Enum<*> -> {
                if (appendTypeCast) {
                    tryExpandAsDynamicDto(paramValue) ?: createEnumParameter(paramValue)
                } else {
                    createEnumParameter(paramValue)
                }
            }
            isValueClass(paramValue) -> tryExpandAsDynamicDto(paramValue)
                ?: throw TypeRegistryException(
                    messageEnum = TypeRegistryExceptionMessage.KOTLIN_CLASS_NOT_MAPPED,
                    typeName = paramValue::class.qualifiedName,
                    cause = IllegalStateException("Value class must be annotated with @DynamicallyMappable.")
                )
            paramValue is String -> "?" to listOf(paramValue.clean()) // ZMIANA
            else -> "?" to listOf(paramValue) // ZMIANA
        }
    }

    /**
     * Próbuje przekonwertować podaną wartość na wrapper `DynamicDto`, jeśli jest to
     * dozwolone w konfiguracji i jeśli klasa jest oznaczona adnotacją @DynamicallyMappable.
     *
     * @return Wynik ekspansji jako `Pair` lub `null`, jeśli konwersja nie jest możliwa/dozwolona.
     */
    private fun tryExpandAsDynamicDto(
        paramValue: Any
    ): Pair<String, List<Any?>>? {
        if (dynamicDtoStrategy == DynamicDtoSerializationStrategy.EXPLICIT_ONLY) return null
        if (dynamicDtoStrategy == DynamicDtoSerializationStrategy.AUTOMATIC_WHEN_UNAMBIGUOUS && typeRegistry.isPgType(paramValue::class)) return null

        val dynamicTypeName = typeRegistry.getDynamicTypeNameForClass(paramValue::class) ?: return null
        val serializer = typeRegistry.getDynamicSerializer(dynamicTypeName)
        logger.trace { "Dynamically converting ${paramValue::class.simpleName} to dynamic_dto '$dynamicTypeName'" }

        val dynamicDtoWrapper = try {
            DynamicDto.from(paramValue, dynamicTypeName, serializer)
        } catch (ex: Exception) {
            logger.error(ex) { ex }; throw ex
        }
        return expandParameter(dynamicDtoWrapper)
    }

    /**
     * Waliduje tablicę typowaną (`Array<*>`) przeznaczoną do bezpośredniego przekazania do JDBC.
     * Rzuca wyjątek, jeśli typ elementów nie jest typem prostym.
     */
    private fun validateTypedArrayParameter(arrayValue: Array<*>): Pair<String, List<Any?>> {
        val componentType = arrayValue::class.java.componentType!!.kotlin
        if (isComplexComponentType(componentType)) {
            throw ConversionException(
                ConversionExceptionMessage.UNSUPPORTED_COMPONENT_TYPE_IN_ARRAY,
                arrayValue,
                targetType = componentType.qualifiedName ?: "unknown"
            )
        }
        return "?" to listOf(arrayValue)
    }

    /**
     * Sprawdza, czy KClass reprezentuje typ złożony (np. data class),
     * który nie może być użyty w tablicy typowanej JDBC.
     */
    private fun isComplexComponentType(kClass: KClass<*>): Boolean {
        // Zwraca true, jeśli typ jest "złożony"
        return kClass.isData || kClass == Map::class || kClass == List::class
    }

    /** Tworzy parametr dla enuma, mapując `CamelCase` na `snake_case` dla typu i wartości. */
    private fun createEnumParameter(enumValue: Enum<*>): Pair<String, List<Any?>> {
        val dbTypeName = typeRegistry.getPgTypeNameForClass(enumValue::class)
        val typeInfo = typeRegistry.getEnumDefinition(dbTypeName)
        val finalDbValue = typeInfo.enumToValueMap[enumValue]
        val pgObject = PGobject().apply {
            value = finalDbValue; type = dbTypeName
        }
        return "?" to listOf(pgObject)
    }

    /**
     * Rozszerza listę na konstrukcję ARRAY[...] PostgreSQL.
     *
     * Rekurencyjnie przetwarza elementy listy, obsługując zagnieżdżone struktury.
     * Pusta lista zostaje przekonwertowana na '{}' (pusta tablica PostgreSQL).
     *
     * @param paramName Nazwa parametru bazowego.
     * @param arrayValue Lista do konwersji.
     * @return Para: placeholder ARRAY[...] i mapa parametrów elementów.
     */
    private fun expandArrayParameter(arrayValue: List<*>): Pair<String, List<Any?>> {
        if (arrayValue.isEmpty()) {
            return "'{}'" to emptyList()
        }

        val expandedParams = mutableListOf<Any?>()
        val placeholders = arrayValue.map { value ->
            val (placeholder, params) = expandParameter(value)
            expandedParams.addAll(params)
            placeholder
        }

        val arrayPlaceholder = "ARRAY[${placeholders.joinToString(", ")}]"
        return arrayPlaceholder to expandedParams
    }

    /**
     * Rozszerza data class na konstrukcję ROW(...)::type_name PostgreSQL.
     *
     * Mapuje pola data class na atrybuty typu kompozytowego w kolejności
     * określonej w TypeRegistry. Rekurencyjnie przetwarza zagnieżdżone pola.
     *
     * @param paramName Nazwa parametru bazowego.
     * @param compositeValue Instancja data class do konwersji.
     * @return Para: placeholder ROW(...)::type_name i mapa parametrów pól.
     * @throws org.octavius.data.exception.TypeRegistryException jeśli klasa nie jest zarejestrowana.
     */
    private fun expandRowParameter(
        compositeValue: Any,
        appendTypeCast: Boolean = true
    ): Pair<String, List<Any?>> {
        val kClass = compositeValue::class
        val dbTypeName = typeRegistry.getPgTypeNameForClass(kClass)
        val typeInfo = typeRegistry.getCompositeDefinition(dbTypeName)
        val valueMap = compositeValue.toMap()
        val expandedParams = mutableListOf<Any?>()

        val placeholders = typeInfo.attributes.keys.map { dbAttributeName ->
            val value = valueMap[dbAttributeName]
            val (placeholder, params) = expandParameter(value)
            expandedParams.addAll(params)
            placeholder
        }

        val rowPlaceholder = if (appendTypeCast) {
            "ROW(${placeholders.joinToString(", ")})::$dbTypeName"
        } else {
            "ROW(${placeholders.joinToString(", ")})"
        }

        return rowPlaceholder to expandedParams
    }

    /**
     * Sprawdza, czy obiekt jest instancją data class.
     *
     * @param obj Obiekt do sprawdzenia.
     * @return true jeśli obj jest instancją data class, false w przeciwnym razie.
     */
    private fun isDataClass(obj: Any): Boolean {
        return obj::class.isData
    }

    /**
     * Sprawdza, czy obiekt jest instancją value class.
     *
     * @param obj Obiekt do sprawdzenia.
     * @return true jeśli obj jest instancją value class, false w przeciwnym razie.
     */
    private fun isValueClass(obj: Any): Boolean {
        return obj::class.isValue
    }
}
