package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.octavius.data.contract.EnumCaseConvention
import org.octavius.util.Converters
import org.octavius.util.toDataObject
import java.text.ParseException
import java.util.UUID
import kotlin.reflect.full.primaryConstructor
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
class PostgresToKotlinConverter(private val typeRegistry: TypeRegistry) {

    private val logger = KotlinLogging.logger {}


    /**
     * Główna funkcja konwertująca, delegująca do specjalistycznych handlerów.
     *
     * @param value Wartość z bazy danych jako `String` (może być `null`).
     * @param pgTypeName Nazwa typu w PostgreSQL (np. "int4", "my_enum").
     * @return Przekonwertowana wartość lub `null`.
     * @throws IllegalArgumentException jeśli typ jest nieznany.
     */
    fun convertToDomainType(value: String?, pgTypeName: String): Any? {
        if (value == null) {
            logger.trace { "Converting null value for type: $pgTypeName" }
            return null
        }
        
        logger.trace { "Converting value '$value' from PostgreSQL type: $pgTypeName" }
        val typeInfo = typeRegistry.getTypeInfo(pgTypeName)
            ?: throw IllegalArgumentException("Nieznany typ PostgreSQL: $pgTypeName")

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
                    ?: throw IllegalStateException("Domena '${typeInfo.typeName}' nie ma zdefiniowanego typu bazowego w TypeRegistry.")
                logger.trace { "Converting domain value '$value' using base type: $baseTypeName" }
                // Wywołujemy tę samą funkcję, ale już dla typu bazowego (np. 'int4', 'bool').
                convertToDomainType(value, baseTypeName)
            }
        }
    }

    /** Konwertuje standardowe typy PostgreSQL (np. int4, text, bool, timestamp). */
    @OptIn(ExperimentalTime::class)
    private fun convertStandardType(value: String, pgTypeName: String): Any? {
        return when (pgTypeName) {
            // Typy numeryczne całkowite
            "int4", "serial", "int2" -> value.toInt()
            "int8" -> value.toLong()

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

            // Interwały czasowe (format HH:MM:SS) TODO format z dniami
            "interval" -> {
                val parts = value.split(":")
                parts[0].toLong().hours +
                        parts[1].toLong().minutes +
                        parts[2].toLong().seconds
            }

            // Typy daty i czasu
            "date" -> LocalDate.Companion.parse(value)
            "timestamp" -> LocalDateTime.Companion.parse(value.replace(' ', 'T'))
            "timestamptz" -> Instant.Companion.parse(value.replace(' ', 'T'))

            // Domyślnie wszystkie inne typy (text, varchar, char) jako String
            else -> value
        }
    }

    /** Konwertuje enum PostgreSQL na enum Kotlina, mapując `snake_case` na `CamelCase`. */
    private fun convertEnum(value: String, typeInfo: PostgresTypeInfo): Any? {
        val enumClassName = typeRegistry.getClassFullPathForPgTypeName(typeInfo.typeName)
            ?: throw IllegalStateException("Nie znaleziono klasy enum dla typu PostgreSQL '${typeInfo.typeName}'.")

        return try {
            val enumClass = Class.forName(enumClassName)

            val enumValueName = when (typeInfo.enumConvention) {
                EnumCaseConvention.SNAKE_CASE_LOWER -> Converters.toCamelCase(value, true)
                EnumCaseConvention.SNAKE_CASE_UPPER -> Converters.toCamelCase(value, true)
                EnumCaseConvention.PASCAL_CASE -> value
                EnumCaseConvention.CAMEL_CASE -> Converters.toCamelCase(value, true)
                EnumCaseConvention.AS_IS -> value
            }
            
            logger.trace { "Converting enum '$value' to '$enumValueName' using convention: ${typeInfo.enumConvention}" }
            val method = enumClass.getMethod("valueOf", String::class.java)
            val result = method.invoke(null, enumValueName)
            logger.trace { "Successfully converted enum value to: $result" }
            result
        } catch (e: Exception) {
            logger.error(e) { "Nie można przekonwertować wartości enum: $value dla typu ${typeInfo.typeName} (próbowano: $enumClassName)" }
            null
        }
    }

    /** Konwertuje tablicę PostgreSQL na `List<Any?>`, rekurencyjnie przetwarzając elementy. */
    private fun convertArray(value: String, typeInfo: PostgresTypeInfo): List<Any?> {
        val elementType = typeInfo.elementType
            ?: throw IllegalStateException("Typ tablicowy ${typeInfo.typeName} nie ma zdefiniowanego typu elementu.")

        logger.trace { "Parsing PostgreSQL array with element type: $elementType" }
        val elements = parsePostgresArray(value)
        logger.trace { "Parsed ${elements.size} array elements" }

        // Rekurencyjnie konwertujemy każdy element tablicy używając głównej funkcji konwersji
        return elements.map { elementValue ->
            // Sprawdzamy, czy string reprezentujący element SAM jest tablicą.
            val isNestedArray = elementValue?.trim()?.startsWith('{') ?: false

            // Jeśli to zagnieżdżona tablica, rekurencyjnie wywołujemy konwersję
            // dla CAŁEGO typu tablicowego (np. "_text"), a nie dla jego elementu ("text").
            // W przeciwnym razie, kontynuujemy standardową logikę z elementType.
            val typeNameToUse = if (isNestedArray) typeInfo.typeName else elementType

            convertToDomainType(elementValue, typeNameToUse)
        }
    }

    /**
     * Konwertuje typ kompozytowy PostgreSQL na `data class` Kotlina.
     * Odpowiada za parsowanie stringa z PG i przetłumaczenie nazw atrybutów
     * z konwencji snake_case na camelCase, tworząc mapę gotową dla `toDataObject`.
     */
    private fun convertCompositeType(value: String, typeInfo: PostgresTypeInfo): Any? {
        val fullClassName = typeRegistry.getClassFullPathForPgTypeName(typeInfo.typeName)
            ?: throw IllegalStateException("Nie znaleziono klasy Kotlina dla typu PostgreSQL '${typeInfo.typeName}'.")

        logger.trace { "Converting composite type ${typeInfo.typeName} to class: $fullClassName" }
        
        // 1. Parsowanie stringa na listę surowych wartości
        val fieldValues = parsePostgresComposite(value)
        val dbAttributes = typeInfo.attributes.toList()

        if (fieldValues.size != dbAttributes.size) {
            logger.error { "Field count mismatch for type ${typeInfo.typeName}: got ${fieldValues.size}, expected ${dbAttributes.size}" }
            throw IllegalArgumentException("Zła ilość pól (${fieldValues.size}) dla typu: ${typeInfo.typeName}, oczekiwano ${dbAttributes.size}")
        }
        
        logger.trace { "Converting ${dbAttributes.size} composite fields" }
        val constructorArgsMap = dbAttributes.mapIndexed { index, (dbAttributeName, dbAttributeType) ->
            val convertedValue = convertToDomainType(fieldValues[index], dbAttributeType)
            dbAttributeName to convertedValue
        }.toMap()

        return try {
            val clazz = Class.forName(fullClassName).kotlin
            // 3. Przekazanie idealnie przygotowanej mapy do uniwersalnego konstruktora
            val result = constructorArgsMap.toDataObject(clazz)
            logger.trace { "Successfully created instance of ${clazz.simpleName}" }
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to create instance for type: ${typeInfo.typeName}" }
            throw IllegalStateException("Nie można utworzyć instancji dla typu: ${typeInfo.typeName}", e)
        }
    }

    // =================================================================
    // --- PARSERY STRUKTUR POSTGRESQL ---
    // =================================================================

    private fun parsePostgresArray(pgArrayString: String): List<String?> = parseNestedStructure(pgArrayString, '{', '}')
    private fun parsePostgresComposite(pgCompositeString: String): List<String?> = parseNestedStructure(pgCompositeString, '(', ')')

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