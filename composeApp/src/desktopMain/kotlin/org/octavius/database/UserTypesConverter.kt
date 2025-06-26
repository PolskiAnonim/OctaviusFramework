package org.octavius.database

import kotlinx.serialization.json.Json.Default.parseToJsonElement
import org.octavius.util.Converters
import java.text.ParseException
import kotlin.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Konwerter wartości z PostgreSQL na typy Kotlin/domenowe.
 * 
 * Odpowiada za konwersję wartości z PostgreSQL (w formie String) na odpowiednie typy Kotlin.
 * Obsługuje wszystkie kategorie typów: standardowe, enum, kompozytowe i tablicowe.
 * 
 * @param typeRegistry Rejestr typów PostgreSQL do uzyskania metadanych typów
 * 
 * @see TypeRegistry
 */
class UserTypesConverter(private val typeRegistry: TypeRegistry) {

    /**
     * Główna funkcja konwertująca wartości PostgreSQL na typy Kotlin/domenowe.
     * 
     * Punkt wejścia dla wszystkich konwersji. Deleguje konwersję do odpowiednich funkcji
     * na podstawie kategorii typu zdefiniowanej w [TypeRegistry].
     * 
     * @param value Wartość do konwersji (stringowa reprezentacja z PostgreSQL)
     * @param pgTypeName Nazwa typu w PostgreSQL
     * @return Przekonwertowana wartość lub null dla wartości NULL
     * 
     * @throws IllegalArgumentException jeśli typ PostgreSQL nie jest znany
     */
    fun convertToDomainType(value: String?, pgTypeName: String): Any? {
        if (value == null) return null

        val typeInfo = typeRegistry.getTypeInfo(pgTypeName)
            ?: throw IllegalArgumentException("Nieznany typ PostgreSQL: $pgTypeName")

        return when (typeInfo.typeCategory) {
            TypeCategory.ENUM -> convertEnum(value, typeInfo)
            TypeCategory.ARRAY -> convertArray(value, typeInfo)
            TypeCategory.COMPOSITE -> convertCompositeType(value, typeInfo)
            TypeCategory.STANDARD -> convertStandardType(value, pgTypeName)
        }
    }

    /**
     * Konwertuje standardowe typy PostgreSQL z ich stringowej reprezentacji na typy Kotlin.
     * 
     * Mapowanie:
     * - int4, serial, int2 → Int
     * - int8 → Long
     * - float4 → Float
     * - float8, numeric → Double
     * - bool → Boolean
     * - json, jsonb, uuid, interval, date, timestamp, timestamptz → String
     * 
     * @param value Wartość do konwersji
     * @param pgTypeName Nazwa typu PostgreSQL
     * @return Przekonwertowana wartość lub null jeśli konwersja się nie powiedzie
     */
    @OptIn(ExperimentalTime::class)
    private fun convertStandardType(value: String, pgTypeName: String): Any? {
        // Zawsze operujemy na stringu
        return when (pgTypeName) {
            // Typy numeryczne
            "int4", "serial", "int2" -> value.toIntOrNull()
            "int8" -> value.toLongOrNull()
            "float4" -> value.toFloatOrNull()
            "float8", "numeric" -> value.toDoubleOrNull()
            // Inne
            "bool" -> value == "t"
            "json", "jsonb" -> parseToJsonElement(value)
            "uuid" -> UUID.fromString(value)
            "interval" -> {
                val parts = value.split(":")
                parts[0].toLong().hours + 
                parts[1].toLong().minutes + 
                parts[2].toLong().seconds
            }
            "date" ->  LocalDate.parse(value)
            "timestamp" ->  LocalDateTime.parse(value)
            "timestamptz" -> Instant.parse(value.replace(' ','T'))
            //"text", "varchar", "char",
            else -> value // Domyślnie zwracamy string
        }

    }

    /**
     * Konwertuje typ enum PostgreSQL na odpowiedni enum Kotlin.
     * 
     * Proces konwersji:
     * 1. Konwertuje nazwę typu z snake_case na CamelCase
     * 2. Znajduje pełną ścieżkę klasy enum
     * 3. Konwertuje wartość enum z snake_case na CamelCase
     * 4. Tworzy instancję enum używając refleksji
     * 
     * @param value Wartość enum z PostgreSQL (np. "reading")
     * @param typeInfo Informacje o typie enum
     * @return Instancja enum lub null jeśli konwersja się nie powiedzie
     * 
     * @see Converters.snakeToCamelCase
     */
    private fun convertEnum(value: String, typeInfo: PostgresTypeInfo): Any? {
        val className = Converters.snakeToCamelCase(typeInfo.typeName, true)
        val enumClassName = typeRegistry.findClassPath(className)

        return try {
            val enumClass = Class.forName(enumClassName)
            val enumValueName = Converters.snakeToCamelCase(value, true)
            val method = enumClass.getMethod("valueOf", String::class.java)
            method.invoke(null, enumValueName)
        } catch (e: Exception) {
            println("Nie można przekonwertować wartości enum: $value dla typu ${typeInfo.typeName} (próbowano: $enumClassName)")
            e.printStackTrace()
            null
        }
    }

    /**
     * Konwertuje tablicę PostgreSQL na List Kotlin.
     * 
     * Parsuje stringową reprezentację tablicy PostgreSQL (format: {val1,val2,val3})
     * i rekurencyjnie konwertuje każdy element tablicy na odpowiedni typ.
     * 
     * @param value Stringowa reprezentacja tablicy PostgreSQL
     * @param typeInfo Informacje o typie tablicy
     * @return Lista przekonwertowanych elementów
     * 
     * @throws IllegalStateException jeśli typ tablicy nie ma zdefiniowanego typu elementu
     */
    private fun convertArray(value: String, typeInfo: PostgresTypeInfo): List<Any?> {
        val elementType = typeInfo.elementType
            ?: throw IllegalStateException("Typ tablicowy ${typeInfo.typeName} nie ma zdefiniowanego typu elementu.")

        // Używamy naszego nowego, niezawodnego parsera
        val elements = parsePostgresArray(value)

        // Rekurencyjnie konwertujemy każdy element tablicy
        return elements.map { elementValue ->
            convertToDomainType(elementValue, elementType)
        }
    }

    /**
     * Konwertuje typ kompozytowy PostgreSQL na odpowiednią klasę Kotlin.
     * 
     * Proces konwersji:
     * 1. Parsuje stringową reprezentację typu kompozytowego (format: (val1,val2,val3))
     * 2. Konwertuje każde pole na odpowiedni typ
     * 3. Tworzy instancję klasy używając refleksji i konstruktora
     * 
     * @param value Stringowa reprezentacja typu kompozytowego
     * @param typeInfo Informacje o typie kompozytowym
     * @return Instancja klasy lub null jeśli konwersja się nie powiedzie
     * 
     * @throws IllegalStateException jeśli typ nie ma zdefiniowanych atrybutów
     * @throws IllegalArgumentException jeśli liczba pól nie zgadza się z liczbą atrybutów
     */
    private fun convertCompositeType(value: String, typeInfo: PostgresTypeInfo): Any? {
        val attributes = typeInfo.attributes
        if (attributes.isEmpty()) {
            throw IllegalStateException("Brak zdefiniowanych atrybutów dla typu kompozytowego ${typeInfo.typeName}")
        }

        // Używamy naszego nowego, niezawodnego parsera
        val fields = parsePostgresComposite(value)

        if (fields.size != attributes.size) {
            println("Ostrzeżenie: liczba pól (${fields.size}) nie zgadza się z liczbą atrybutów (${attributes.size}) dla typu ${typeInfo.typeName}")
            // Można rzucić wyjątkiem lub próbować kontynuować
            throw IllegalArgumentException("Zła ilość pól dla typu: ${typeInfo.typeName}")
        }

        val attributeTypes = attributes.values.toList()

        // Przekształć każde pole na docelowy typ, wywołując rekurencyjnie główną funkcję
        val constructorArgs = fields.mapIndexed { index, fieldValue ->
            val attributeType = attributeTypes.getOrNull(index)
                ?: throw IllegalStateException("Brak typu dla atrybutu o indeksie $index w ${typeInfo.typeName}")
            convertToDomainType(fieldValue, attributeType)
        }.toTypedArray()

        return try {
            val className = Converters.snakeToCamelCase(typeInfo.typeName, true)
            val fullClassName = typeRegistry.findClassPath(className)
            val clazz = Class.forName(fullClassName)
            val constructor = clazz.constructors.first() // Zakładamy, że jest jeden konstruktor
            constructor.newInstance(*constructorArgs)
        } catch (e: Exception) {
            println("Nie można utworzyć instancji obiektu dla typu kompozytowego: ${typeInfo.typeName} z wartością $value (próbowano: ${typeRegistry.findClassPath(Converters.snakeToCamelCase(typeInfo.typeName, true))})")
            e.printStackTrace()
            null
        }
    }

    // =================================================================
    // === ZINTEGROWANE FUNKCJE PARSERA (jako prywatne metody) ===
    // =================================================================

    /**
     * Parsuje stringową reprezentację tablicy PostgreSQL.
     * 
     * @param pgArrayString Reprezentacja tablicy w formacie PostgreSQL (np. "{val1,val2,val3}")
     * @return Lista elementów tablicy
     */
    private fun parsePostgresArray(pgArrayString: String): List<String?> {
        return _parseNestedStructure(pgArrayString, '{', '}')
    }

    /**
     * Parsuje stringową reprezentację typu kompozytowego PostgreSQL.
     * 
     * @param pgCompositeString Reprezentacja typu kompozytowego w formacie PostgreSQL (np. "(val1,val2,val3)")
     * @return Lista pól typu kompozytowego
     */
    private fun parsePostgresComposite(pgCompositeString: String): List<String?> {
        return _parseNestedStructure(pgCompositeString, '(', ')')
    }

    /**
     * Uniwersalny parser dla zagnieżdżonych struktur PostgreSQL.
     * 
     * Obsługuje:
     * - Cudzysłowy i escape'owanie
     * - Zagnieżdżone struktury (tablice w tablicach, typy kompozytowe w tablicach)
     * - Wartości NULL
     * 
     * @param input Wejściowy string do sparsowania
     * @param startChar Znak początkowy struktury ('{' dla tablic, '(' dla typów kompozytowych)
     * @param endChar Znak końcowy struktury ('}' dla tablic, ')' dla typów kompozytowych)
     * @return Lista sparsowanych elementów
     * 
     * @throws ParseException jeśli format jest nieprawidłowy
     */
    private fun _parseNestedStructure(input: String, startChar: Char, endChar: Char): List<String?> {
        val trimmed = input.trim()
        if (!trimmed.startsWith(startChar) || !trimmed.endsWith(endChar)) {
            throw ParseException("Nieprawidłowy format: Oczekiwano '$startChar...$endChar'", 0)
        }
        val content = trimmed.substring(1, trimmed.length - 1)
        if (content.isEmpty()) return emptyList()

        val elements = mutableListOf<String>()
        var currentElementStart = 0
        var inQuotes = false
        var braceLevel = 0
        var parenLevel = 0

        for (i in content.indices) {
            val char = content[i]
            if (inQuotes) {
                if (char == '\\') continue
                if (char == '"') inQuotes = false
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    '{' -> braceLevel++
                    '}' -> braceLevel--
                    '(' -> parenLevel++
                    ')' -> parenLevel--
                    ',' -> {
                        if (braceLevel == 0 && parenLevel == 0) {
                            elements.add(content.substring(currentElementStart, i))
                            currentElementStart = i + 1
                        }
                    }
                }
            }
        }
        elements.add(content.substring(currentElementStart))
        return elements.map { unescapeValue(it.trim()) }
    }

    /**
     * Usuwa escape'owanie z wartości PostgreSQL.
     * 
     * Obsługuje:
     * - Wartości NULL (niezależnie od wielkości liter)
     * - Cudzysłowy i ich escape'owanie
     * - Backslash escaping
     * 
     * @param raw Surowa wartość do przetworzenia
     * @return Przetworzona wartość lub null dla NULL
     */
    private fun unescapeValue(raw: String): String? {
        if (raw.equals("NULL", ignoreCase = true)) return null
        if (raw.startsWith('"') && raw.endsWith('"')) {
            return raw.substring(1, raw.length - 1)
                .replace("\"\"", "\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return raw
    }
}