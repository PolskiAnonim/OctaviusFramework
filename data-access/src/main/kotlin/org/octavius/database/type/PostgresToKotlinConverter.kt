package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import org.octavius.data.EnumCaseConvention
import org.octavius.data.toDataObject
import org.octavius.exception.DataConversionException
import org.octavius.exception.DataMappingException
import org.octavius.exception.TypeRegistryException
import org.octavius.util.Converters
import org.octavius.util.OffsetTime
import java.lang.reflect.Method
import java.text.ParseException
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Konwertuje wartości z PostgreSQL (jako `String`) na odpowiednie typy Kotlina.
 *
 * Obsługuje typy standardowe, enumy, kompozyty i tablice, korzystając z metadanych
 * z `TypeRegistry` do dynamicznego mapowania.
 *
 * @param typeRegistry Rejestr zawierający metadane o typach PostgreSQL.
 */
internal class PostgresToKotlinConverter(private val typeRegistry: TypeRegistry) {
    companion object {
        private val logger = KotlinLogging.logger {}

        // Cache dla klasy enum i metody valueOf
        private val enumClassCache = ConcurrentHashMap<String, Method>()

        // Cache dla Kotlin KClass kompozytowych (data class)
        private val compositeKClassCache = ConcurrentHashMap<String, KClass<*>>()
    }

    val POSTGRES_TIMETZ_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
        // Parsuj część czasową (godzina, minuta, sekunda)
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .optionalStart() // Sekundy są opcjonalne
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalEnd()
        .appendPattern("X")
        .toFormatter()

    /**
     * Główna funkcja konwertująca, delegująca do specjalistycznych handlerów.
     *
     * Obsługuje wszystkie kategorie typów: STANDARD, ENUM, ARRAY, COMPOSITE i DOMAIN.
     * Dla typów domenowych rekurencyjnie deleguje do typu bazowego.
     *
     * @param value Wartość z bazy danych jako `String` (może być `null`).
     * @param pgTypeName Nazwa typu w PostgreSQL (np. "int4", "my_enum", "_text").
     * @return Przekonwertowana wartość lub `null` jeśli `value` było `null`.
     * @throws TypeRegistryException jeśli typ jest nieznany lub nie ma typu bazowego.
     * @throws DataConversionException jeśli konwersja się nie powiedzie.
     */
    fun convert(value: String?, pgTypeName: String): Any? {
        if (value == null) {
            logger.trace { "Converting null value for type: $pgTypeName" }
            return null
        }

        logger.trace { "Converting value '$value' from PostgreSQL type: $pgTypeName" }
        val typeInfo = typeRegistry.getTypeInfo(pgTypeName)

        return when (typeInfo.typeCategory) {
            TypeCategory.ENUM -> {
                logger.trace { "Converting enum value '$value' for type $pgTypeName" }
                convertEnum(value, typeInfo)
            }

            TypeCategory.ARRAY -> {
                logger.trace { "Converting array value for type $pgTypeName" }
                convertArray(value, typeInfo)
            }

            TypeCategory.COMPOSITE -> {
                logger.trace { "Converting composite value for type $pgTypeName" }
                convertCompositeType(value, typeInfo)
            }

            TypeCategory.STANDARD -> {
                logger.trace { "Converting standard value '$value' for type $pgTypeName" }
                convertStandardType(value, pgTypeName)
            }

            TypeCategory.DOMAIN -> {
                val baseTypeName = typeInfo.baseTypeName
                    ?: throw TypeRegistryException("Domena '${typeInfo.typeName}' nie ma zdefiniowanego typu bazowego w TypeRegistry.")
                logger.trace { "Converting domain value '$value' using base type: $baseTypeName" }
                // Wywołujemy tę samą funkcję, ale już dla typu bazowego (np. 'int4', 'bool').
                convert(value, baseTypeName)
            }
        }
    }

    /**
     * Konwertuje standardowe typy PostgreSQL na odpowiednie typy Kotlina.
     *
     * Obsługiwane typy:
     * - Numeryczne: int4, int8, float4, float8, numeric
     * - Logiczne: bool (t/f -> Boolean)
     * - Tekstowe: text, varchar, char (-> String)
     * - Data/czas: date, timestamp, timestamptz, time, timetz
     * - JSON: json, jsonb (-> JsonElement)
     * - Inne: uuid, interval
     *
     * @param value Wartość z bazy danych jako String.
     * @param pgTypeName Nazwa standardowego typu PostgreSQL.
     * @return Przekonwertowana wartość.
     * @throws DataConversionException jeśli konwersja się nie powiedzie.
     */
    @OptIn(ExperimentalTime::class)
    private fun convertStandardType(value: String, pgTypeName: String): Any? {
        try {
            return when (pgTypeName) {
                // Typy numeryczne całkowite
                "int4", "serial", "int2", "smallserial" -> value.toInt()
                "int8", "bigserial" -> value.toLong()

                // Typy zmiennoprzecinkowe
                "float4" -> value.toFloat()
                "float8" -> value.toDouble()
                "numeric" -> value.toBigDecimal()

                // Wartości logiczne (PostgreSQL używa 't'/'f')
                "bool" -> value == "t"

                // Typy JSON
                "json", "jsonb" -> Json.parseToJsonElement(value)

                // UUID
                "uuid" -> UUID.fromString(value)

                // Interwały czasowe (format HH:MM:SS) TODO obsługa miesięcy/dni (jeżeli zajdzie potrzeba
                "interval" -> {
                    val parts = value.split(":")
                    parts[0].toLong().hours +
                            parts[1].toLong().minutes +
                            parts[2].toLong().seconds
                }

                // Typy daty i czasu
                "date" -> LocalDate.parse(value)
                "timestamp" -> LocalDateTime.parse(value.replace(' ', 'T'))
                "timestamptz" -> Instant.parse(value.replace(' ', 'T'))
                "time" -> LocalTime.parse(value)
                "timetz" -> {
                    val javaOffsetTime = java.time.OffsetTime.parse(value, POSTGRES_TIMETZ_FORMATTER)
                    return OffsetTime(
                        time = javaOffsetTime.toLocalTime().toKotlinLocalTime(),
                        offset = UtcOffset(seconds = javaOffsetTime.offset.totalSeconds)
                    )
                }
                // Domyślnie wszystkie inne typy (text, varchar, char) jako String
                else -> value
            }
        } catch (e: Exception) {
            throw DataConversionException(
                message = "Nie można przekonwertować wartości '$value' na docelowy typ dla PostgreSQL typu '$pgTypeName'.",
                value = value,
                targetType = "N/A (deduced from $pgTypeName)",
                cause = e
            )
        }
    }

    /**
     * Konwertuje wartość enum z PostgreSQL na enum Kotlina.
     *
     * Mapuje nazwy wartości według konwencji określonej w TypeRegistry:
     * - SNAKE_CASE_LOWER/UPPER -> CamelCase
     * - PASCAL_CASE -> bez zmian
     * - AS_IS -> bez zmian
     *
     * @param value Wartość enum z bazy danych.
     * @param typeInfo Informacje o typie enum z TypeRegistry.
     * @return Instancja enum Kotlina.
     * @throws TypeRegistryException jeśli nie znaleziono klasy enum.
     * @throws DataConversionException jeśli konwersja się nie powiedzie.
     */
    private fun convertEnum(value: String, typeInfo: PostgresTypeInfo): Any? {
        val enumClassName = typeRegistry.getClassFullPathForPgTypeName(typeInfo.typeName)
            ?: throw TypeRegistryException("Nie znaleziono klasy enum dla typu PostgreSQL '${typeInfo.typeName}'.")

        return try {
            // Użycie cache'a
            val valueOfMethod = enumClassCache.getOrPut(enumClassName) {
                logger.debug { "Cache miss for enum class $enumClassName. Loading via reflection." }
                val clazz = Class.forName(enumClassName)
                clazz.getMethod("valueOf", String::class.java)
            }

            val enumValueName = when (typeInfo.enumConvention) {
                EnumCaseConvention.SNAKE_CASE_LOWER -> Converters.toCamelCase(value, true)
                EnumCaseConvention.SNAKE_CASE_UPPER -> Converters.toCamelCase(value, true)
                EnumCaseConvention.PASCAL_CASE -> value
                EnumCaseConvention.CAMEL_CASE -> Converters.toCamelCase(value, true)
                EnumCaseConvention.AS_IS -> value
            }

            logger.trace { "Converting enum '$value' to '$enumValueName' using convention: ${typeInfo.enumConvention}" }
            val result = valueOfMethod.invoke(null, enumValueName)
            logger.trace { "Successfully converted enum value to: $result" }
            result
        } catch (e: Exception) {
            val conversionEx = DataConversionException(
                message = "Nie można przekonwertować wartości enum '$value' dla typu '${typeInfo.typeName}' na klasę '$enumClassName'",
                value = value,
                targetType = enumClassName,
                cause = e
            )
            logger.error(conversionEx) { conversionEx.message }
            throw conversionEx
        }
    }

    /**
     * Konwertuje tablicę PostgreSQL na `List<Any?>`.
     *
     * Obsługuje zagnieżdżone tablice i rekurencyjnie przetwarza elementy
     * zgodnie z typem elementu określonym w TypeRegistry.
     *
     * @param value String reprezentujący tablicę PostgreSQL (format: {elem1,elem2,...}).
     * @param typeInfo Informacje o typie tablicowym z TypeRegistry.
     * @return Lista przekonwertowanych elementów.
     * @throws TypeRegistryException jeśli brak informacji o typie elementu.
     * @throws DataConversionException jeśli parsowanie się nie powiedzie.
     */
    private fun convertArray(value: String, typeInfo: PostgresTypeInfo): List<Any?> {
        val elementType = typeInfo.elementType
            ?: throw TypeRegistryException("Typ tablicowy ${typeInfo.typeName} nie ma zdefiniowanego typu elementu.")

        logger.trace { "Parsing PostgreSQL array with element type: $elementType" }
        val elements: List<String?>
        try {
            elements = parsePostgresArray(value)
        } catch (e: Exception) {
            val ex = DataConversionException(
                "Error parsing PostgreSQL Array",
                value = value,
                targetType = typeInfo.typeName,
                cause = e
            )
            logger.error(ex) { "Error parsing PostgreSQL Array. Exception: $ex" }
            throw ex
        }

        logger.trace { "Parsed ${elements.size} array elements" }

        // Rekurencyjnie konwertujemy każdy element tablicy używając głównej funkcji konwersji
        return elements.map { elementValue ->
            // Sprawdzamy, czy string reprezentujący element SAM jest tablicą.
            val isNestedArray = elementValue?.trim()?.startsWith('{') ?: false

            // Jeśli to zagnieżdżona tablica, rekurencyjnie wywołujemy konwersję
            // dla CAŁEGO typu tablicowego (np. "_text"), a nie dla jego elementu ("text").
            // W przeciwnym razie, kontynuujemy standardową logikę z elementType.
            val typeNameToUse = if (isNestedArray) typeInfo.typeName else elementType

            convert(elementValue, typeNameToUse)
        }
    }

    /**
     * Konwertuje typ kompozytowy PostgreSQL na `data class` Kotlina.
     * Wykorzystuje cache dla KClass i deleguje do `toDataObject` (które samo ma cache).
     */
    private fun convertCompositeType(value: String, typeInfo: PostgresTypeInfo): Any? {
        val fullClassName = typeRegistry.getClassFullPathForPgTypeName(typeInfo.typeName)
            ?: throw TypeRegistryException("Nie znaleziono klasy Kotlina dla typu PostgreSQL '${typeInfo.typeName}'.")

        logger.trace { "Converting composite type ${typeInfo.typeName} to class: $fullClassName" }

        // 1. Parsowanie stringa na listę surowych wartości
        val fieldValues: List<String?>
        try {
            fieldValues = parsePostgresComposite(value)
        } catch (e: Exception) {
            val conversionEx = DataConversionException(
                "Błąd parsowania kompozytu",
                value = value,
                targetType = typeInfo.typeName,
                cause = e
            )
            logger.error(conversionEx) { "Error parsing PostgreSQL composite type" }
            throw conversionEx
        }

        val dbAttributes = typeInfo.attributes.toList()

        if (fieldValues.size != dbAttributes.size) {
            val ex = DataConversionException(
                message = "Niezgodna liczba pól dla typu kompozytowego ${typeInfo.typeName}: otrzymano ${fieldValues.size}, oczekiwano ${dbAttributes.size}",
                value = value, // cała surowa wartość kompozytu
                targetType = fullClassName
            )
            logger.error(ex) { "Field count mismatch for type ${typeInfo.typeName}: got ${fieldValues.size}, expected ${dbAttributes.size}" }
            throw ex
        }

        logger.trace { "Converting ${dbAttributes.size} composite fields" }
        val constructorArgsMap = dbAttributes.mapIndexed { index, (dbAttributeName, dbAttributeType) ->
            val convertedValue = convert(fieldValues[index], dbAttributeType)
            dbAttributeName to convertedValue
        }.toMap()

        return try {
            // Użycie cache'a dla KClass
            val clazz = compositeKClassCache.getOrPut(fullClassName) {
                logger.trace { "Cache miss for composite KClass $fullClassName. Loading via reflection." }
                Class.forName(fullClassName).kotlin
            }
            val result = constructorArgsMap.toDataObject(clazz)
            logger.trace { "Successfully created instance of ${clazz.simpleName}" }
            result
        } catch (e: Exception) {
            val mappingEx = DataMappingException(
                message = "Nie można utworzyć instancji klasy '$fullClassName' z danych typu kompozytowego '${typeInfo.typeName}'",
                targetClass = fullClassName,
                rowData = constructorArgsMap,
                cause = e
            )
            logger.error(mappingEx) { mappingEx.message }
            throw mappingEx
        }
    }

    // =================================================================
    // --- PARSERY STRUKTUR POSTGRESQL ---
    // =================================================================

    private fun parsePostgresArray(pgArrayString: String): List<String?> = parseNestedStructure(pgArrayString, '{', '}')
    private fun parsePostgresComposite(pgCompositeString: String): List<String?> =
        parseNestedStructure(pgCompositeString, '(', ')')

    /**
     * Uniwersalny parser dla zagnieżdżonych struktur (tablic i kompozytów).
     * Obsługuje cudzysłowy, escapowanie, wartości `NULL` i zagnieżdżenia.
     */
    private fun parseNestedStructure(input: String, startChar: Char, endChar: Char): List<String?> {
        val trimmed = input.trim()
        if (!trimmed.startsWith(startChar) || !trimmed.endsWith(endChar)) {
            throw ParseException("Nieprawidłowy format: Oczekiwano '$startChar...$endChar'", 0)
        }
        val content = trimmed.substring(1, trimmed.length - 1)
        if (content.isEmpty()) return emptyList()

        val elements = mutableListOf<String>()
        var currentElementStart = 0
        var inQuotes = false
        var braceLevel = 0 // Poziom zagnieżdżenia nawiasów klamrowych {}
        var parenLevel = 0 // Poziom zagnieżdżenia nawiasów okrągłych ()

        var i = 0
        while (i < content.length) {
            val char = content[i]
            if (inQuotes) {
                when (char) {
                    '\\' -> i++ // Pomiń następny znak - jest escape'owany
                    '"' -> inQuotes = false
                }
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    '{' -> braceLevel++
                    '}' -> braceLevel--
                    '(' -> parenLevel++
                    ')' -> parenLevel--
                    ',' -> {
                        // Przecinek na najwyższym poziomie = separator elementów
                        if (braceLevel == 0 && parenLevel == 0) {
                            elements.add(content.substring(currentElementStart, i))
                            currentElementStart = i + 1
                        }
                    }
                }
            }
            i++
        }
        elements.add(content.substring(currentElementStart))
        return elements.map { unescapeValue(it.trim()) }
    }

    /**
     * Przetwarza surową wartość, usuwając cudzysłowy i escapowanie.
     * Poprawnie interpretuje `NULL` (jawne `NULL` lub pusty, niecytowany string)
     * oraz pusty string (reprezentowany jako `""`).
     */
    private fun unescapeValue(raw: String): String? {
        val trimmed = raw.trim()

        // 1. Sprawdzamy, czy wartość jest w cudzysłowach.
        if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
            // Jeśli tak, to jest to jawny string. Nawet jeśli pusty (""), to jest to pusty string, a nie NULL.
            return trimmed.substring(1, trimmed.length - 1)
                .replace("\"\"", "\"") // PostgreSQL escapuje cudzysłów przez podwojenie go
                .replace("\\\"", "\"") // Obsługa standardowego escape'owania
                .replace("\\\\", "\\")
        }

        // 2. Jeśli wartość NIE jest w cudzysłowach.
        // Pusty, nieopakowany w cudzysłowy ciąg znaków w kompozycie oznacza NULL.
        if (trimmed.isEmpty() || trimmed.equals("NULL", ignoreCase = true)) {
            return null
        }

        // 3. W każdym innym przypadku jest to zwykła, nieopakowana w cudzysłowy wartość.
        return trimmed
    }
}