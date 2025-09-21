package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonObject
import org.octavius.data.contract.EnumCaseConvention
import org.octavius.data.contract.PgTyped
import org.octavius.exception.TypeRegistryException
import org.octavius.util.Converters
import org.octavius.util.OffsetTime
import org.octavius.util.toMap
import org.postgresql.util.PGobject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

/**
 * Wynik ekspansji zapytania.
 * @param expandedSql Zapytanie z rozszerzonymi placeholderami (np. `ARRAY[...]`, `ROW(...)`).
 * @param expandedParams Mapa spłaszczonych parametrów do użycia w `PreparedStatement`.
 */
data class ExpandedQuery(
    val expandedSql: String,
    val expandedParams: Map<String, Any?>
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
internal class KotlinToPostgresConverter(private val typeRegistry: TypeRegistry) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }


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
    fun expandParametersInQuery(sql: String, params: Map<String, Any?>): ExpandedQuery {
        logger.debug { "Expanding parameters in query. Original params count: ${params.size}" }
        logger.trace { "Original SQL: $sql" }

        var expandedSql = sql
        val expandedParams = mutableMapOf<String, Any?>()

        params.forEach { (paramName, paramValue) ->
            val paramValue = params[paramName]

            val placeholderRegex = Regex(":$paramName\\b")

            // Sprawdzamy, czy placeholder w ogóle istnieje w SQL
            if (placeholderRegex.containsMatchIn(expandedSql)) {
                logger.trace { "Expanding parameter ':$paramName' of type ${paramValue?.javaClass?.simpleName}" }
                // Ekspanduj parametr na odpowiednią konstrukcję SQL
                val (newPlaceholder, newParams) = expandParameter(paramName, paramValue)

                expandedSql = placeholderRegex.replace(expandedSql, newPlaceholder)

                expandedParams.putAll(newParams)
                logger.trace { "Parameter ':$paramName' expanded to: $newPlaceholder" }
            } else {
                expandedParams[paramName] = paramValue
            }
        }

        logger.debug { "Parameter expansion completed. Expanded params count: ${expandedParams.size}" }
        logger.trace { "Expanded SQL: $expandedSql" }
        return ExpandedQuery(expandedSql, expandedParams)
    }

    /**
     * Rozszerza pojedynczy parametr na odpowiednią konstrukcję SQL.
     *
     * @param paramName Nazwa parametru (bez dwukropka).
     * @param paramValue Wartość parametru do konwersji.
     * @return Para: placeholder SQL i mapa spłaszczonych parametrów.
     */
    @OptIn(ExperimentalTime::class)
    private fun expandParameter(paramName: String, paramValue: Any?): Pair<String, Map<String, Any?>> {
        return when {
            paramValue is PgTyped -> {
                val (innerPlaceholder, innerParams) = expandParameter(paramName, paramValue.value)
                // 2. Do wyniku (który może być już np. "ARRAY[:p1, :p2]") doklej rzutowanie.
                val finalPlaceholder = innerPlaceholder + "::" + paramValue.pgType
                finalPlaceholder to innerParams
            }

            paramValue is List<*> -> expandArrayParameter(paramName, paramValue)
            paramValue is LocalDate -> createDateParameter(paramName, paramValue)
            paramValue is LocalDateTime -> createTimestampParameter(paramName, paramValue)
            paramValue is LocalTime -> createTimeParameter(paramName, paramValue)
            paramValue is Instant -> createInstantParameter(paramName, paramValue)
            paramValue is OffsetTime -> createOffsetTimeParameter(paramName, paramValue)
            paramValue is Duration -> createIntervalParameter(paramName, paramValue)
            isDataClass(paramValue) -> expandRowParameter(paramName, paramValue!!)
            paramValue is JsonObject -> createJsonParameter(paramName, paramValue)
            paramValue is Enum<*> -> createEnumParameter(paramName, paramValue)
            else -> ":$paramName" to mapOf(paramName to paramValue)
        }
    }

    /** Konwertuje `LocalDate` na `java.sql.Date`. */
    private fun createDateParameter(paramName: String, value: LocalDate): Pair<String, Map<String, Any?>> {
        val sqlDate = java.sql.Date.valueOf(value.toJavaLocalDate())
        return ":$paramName" to mapOf(paramName to sqlDate)
    }

    /** Konwertuje `LocalDateTime` na `java.sql.Timestamp`. */
    private fun createTimestampParameter(paramName: String, value: LocalDateTime): Pair<String, Map<String, Any?>> {
        val sqlTimestamp = java.sql.Timestamp.valueOf(value.toJavaLocalDateTime())
        return ":$paramName" to mapOf(paramName to sqlTimestamp)
    }

    /** Konwertuje `LocalTime` na `java.sql.Time` dla kolumn typu TIME. */
    private fun createTimeParameter(paramName: String, value: LocalTime): Pair<String, Map<String, Any?>> {
        val sqlTime = java.sql.Time.valueOf(value.toJavaLocalTime())
        return ":$paramName" to mapOf(paramName to sqlTime)
    }

    /** Konwertuje `Instant` na `java.sql.Timestamp`. */
    @OptIn(ExperimentalTime::class)
    private fun createInstantParameter(paramName: String, value: Instant): Pair<String, Map<String, Any?>> {
        val sqlTimestamp = java.sql.Timestamp.from(value.toJavaInstant())
        return ":$paramName" to mapOf(paramName to sqlTimestamp)
    }

    /** Konwertuje `KotlinOffsetTime` na `java.time.OffsetTime`, z którym JDBC sobie radzi. */
    private fun createOffsetTimeParameter(paramName: String, value: OffsetTime): Pair<String, Map<String, Any?>> {
        val javaOffset = java.time.ZoneOffset.ofTotalSeconds(value.offset.totalSeconds)
        val javaTime = value.time.toJavaLocalTime()
        val offsetTime = java.time.OffsetTime.of(javaTime, javaOffset)
        return ":$paramName" to mapOf(paramName to offsetTime)
    }

    /** Konwertuje `Duration` na `PGobject` typu `interval`. */
    @OptIn(ExperimentalTime::class)
    private fun createIntervalParameter(paramName: String, value: Duration): Pair<String, Map<String, Any?>> {
        val pgInterval = PGobject().apply {
            type = "interval"
            this.value = value.toIsoString() // Format ISO 8601 (np. PT1H30M) jest dobrze obsługiwany przez PG
        }
        return ":$paramName" to mapOf(paramName to pgInterval)
    }

    /** Tworzy parametr `jsonb` z obiektu `JsonObject`. */
    private fun createJsonParameter(paramName: String, paramValue: JsonObject): Pair<String, Map<String, Any?>> {
        val pgObject = PGobject().apply {
            value = paramValue.toString()
            type = "jsonb"
        }
        return ":$paramName" to mapOf(paramName to pgObject)
    }

    /** Tworzy parametr dla enuma, mapując `CamelCase` na `snake_case` dla typu i wartości. */
    private fun createEnumParameter(paramName: String, enumValue: Enum<*>): Pair<String, Map<String, Any?>> {
        val enumKClass = enumValue::class
        logger.trace { "Creating enum parameter for ${enumKClass.simpleName}.${enumValue.name}" }

        val dbTypeName = typeRegistry.getPgTypeNameForClass(enumKClass)

        val typeInfo = typeRegistry.getTypeInfo(dbTypeName)
        val convention = typeInfo.enumConvention
        
        logger.trace { "Converting enum value '${enumValue.name}' using convention: $convention" }
        val finalValue = when (convention) {
            EnumCaseConvention.SNAKE_CASE_LOWER -> Converters.toSnakeCase(enumValue.name).lowercase()
            EnumCaseConvention.SNAKE_CASE_UPPER -> Converters.toSnakeCase(enumValue.name).uppercase()
            EnumCaseConvention.PASCAL_CASE -> enumValue.name
            EnumCaseConvention.CAMEL_CASE -> Converters.toCamelCase(enumValue.name, true)
            EnumCaseConvention.AS_IS -> enumValue.name
        }
        
        logger.trace { "Enum value converted to: '$finalValue' with type: $dbTypeName" }
        val pgObject = PGobject().apply {
            value = finalValue
            type = dbTypeName
        }
        return ":$paramName" to mapOf(paramName to pgObject)
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
    private fun expandArrayParameter(paramName: String, arrayValue: List<*>): Pair<String, Map<String, Any?>> {
        logger.trace { "Expanding array parameter '$paramName' with ${arrayValue.size} elements" }
        
        if (arrayValue.isEmpty()) {
            logger.trace { "Array parameter '$paramName' is empty, using empty array literal" }
            return "'{}'" to emptyMap() // Pusta tablica PostgreSQL
        }

        val expandedParams = mutableMapOf<String, Any?>()
        val placeholders = arrayValue.mapIndexed { index, value ->
            val elementParamName = "${paramName}_p${index + 1}" // Unikalna nazwa dla każdego elementu
            val (placeholder, params) = expandParameter(elementParamName, value)
            expandedParams.putAll(params)
            placeholder
        }

        val arrayPlaceholder = "ARRAY[${placeholders.joinToString(", ")}]"
        logger.trace { "Array parameter '$paramName' expanded to: $arrayPlaceholder" }
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
     * @throws TypeRegistryException jeśli klasa nie jest zarejestrowana.
     */
    private fun expandRowParameter(paramName: String, compositeValue: Any): Pair<String, Map<String, Any?>> {
        val kClass = compositeValue::class
        logger.trace { "Expanding row parameter '$paramName' for class ${kClass.simpleName}" }

        // 1. Pobierz informacje o typie z rejestru (bez zmian)
        val dbTypeName = typeRegistry.getPgTypeNameForClass(kClass)
        val typeInfo = typeRegistry.getTypeInfo(dbTypeName)
        
        logger.trace { "Found database type '$dbTypeName' with ${typeInfo.attributes.size} attributes" }

        val valueMap = compositeValue.toMap()

        val expandedParams = mutableMapOf<String, Any?>()

        // 3. Iteruj po ATRYBUTACH Z BAZY DANYCH, aby ZAGWARANTOWAĆ poprawną kolejność
        val placeholders = typeInfo.attributes.keys.mapIndexed { index, dbAttributeName ->

            val value = valueMap[dbAttributeName]

            val fieldParamName = "${paramName}_f${index + 1}"
            val (placeholder, params) = expandParameter(fieldParamName, value)
            expandedParams.putAll(params)
            placeholder
        }

        val rowPlaceholder = "ROW(${placeholders.joinToString(", ")})::$dbTypeName"
        logger.trace { "Row parameter '$paramName' expanded to: $rowPlaceholder" }

        return rowPlaceholder to expandedParams
    }

    /**
     * Sprawdza, czy obiekt jest instancją data class.
     *
     * @param obj Obiekt do sprawdzenia.
     * @return true jeśli obj jest instancją data class, false w przeciwnym razie.
     */
    private fun isDataClass(obj: Any?): Boolean {
        return obj != null && obj::class.isData
    }
}