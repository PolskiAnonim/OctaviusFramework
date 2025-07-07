package org.octavius.database

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import org.octavius.util.Converters
import java.text.ParseException
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Konwerter wartości z PostgreSQL na typy Kotlin/domenowe.
 *
 * Główna klasa odpowiedzialna za konwersję wartości z PostgreSQL (stringowa reprezentacja)
 * na odpowiednie typy Kotlin. Obsługuje wszystkie kategorie typów zdefiniowane w systemie:
 * - Standardowe typy PostgreSQL (int4, text, bool, json, uuid, date, timestamp, etc.)
 * - Typy enum (konwersja z snake_case na CamelCase)
 * - Typy kompozytowe (obiekty z wieloma polami)
 * - Typy tablicowe (tablice dowolnych typów)
 *
 * Konwerter używa refleksji do tworzenia instancji klas enum i kompozytowych,
 * co umożliwia automatyczne mapowanie bez manualnej konfiguracji.
 *
 * @param typeRegistry Rejestr typów PostgreSQL zawierający metadane o typach
 * @see TypeRegistry
 */
class DatabaseToKotlinTypesConverter(private val typeRegistry: TypeRegistry) {


    /**
     * Główna funkcja konwertująca wartości PostgreSQL na typy Kotlin/domenowe.
     *
     * Punkt wejścia dla wszystkich konwersji. Analizuje kategorię typu na podstawie
     * metadanych z [TypeRegistry] i deleguje konwersję do odpowiednich funkcji
     * specjalistycznych. Obsługuje wszystkie kategorie typów: standardowe, enum,
     * kompozytowe i tablicowe.
     *
     * @param value Wartość do konwersji (stringowa reprezentacja z PostgreSQL)
     * @param pgTypeName Nazwa typu w PostgreSQL (np. "int4", "text", "my_enum")
     * @return Przekonwertowana wartość lub null dla wartości NULL
     * @throws IllegalArgumentException jeśli typ PostgreSQL nie jest znany w rejestrze
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
     * Konwertuje standardowe typy PostgreSQL na odpowiednie typy Kotlin.
     *
     * Obsługuje wszystkie podstawowe typy PostgreSQL używane w aplikacji:
     * - **Liczby całkowite**: int4, serial, int2 → Int | int8 → Long
     * - **Liczby zmiennoprzecinkowe**: float4 → Float | float8, numeric → Double
     * - **Wartości logiczne**: bool → Boolean (PostgreSQL: 't'/'f')
     * - **Daty i czas**: date → LocalDate | timestamp → LocalDateTime | timestamptz → Instant
     * - **JSON**: json, jsonb → JsonElement (kotlinx.serialization)
     * - **UUID**: uuid → UUID
     * - **Interwały**: interval → Duration (kotlin.time)
     * - **Tekst**: text, varchar, char → String (domyślny)
     *
     * @param value Wartość do konwersji (stringowa reprezentacja z PostgreSQL)
     * @param pgTypeName Nazwa typu PostgreSQL
     * @return Przekonwertowana wartość lub null jeśli konwersja się nie powiedzie
     */
    @OptIn(ExperimentalTime::class)
    private fun convertStandardType(value: String, pgTypeName: String): Any? {
        return when (pgTypeName) {
            // Typy numeryczne całkowite
            "int4", "serial", "int2" -> value.toIntOrNull()
            "int8" -> value.toLongOrNull()
            
            // Typy zmiennoprzecinkowe
            "float4" -> value.toFloatOrNull()
            "float8", "numeric" -> value.toDoubleOrNull()
            
            // Wartości logiczne (PostgreSQL używa 't'/'f')
            "bool" -> value == "t"
            
            // Typy JSON
            "json", "jsonb" -> parseToJsonElement(value)
            
            // UUID
            "uuid" -> UUID.fromString(value)
            
            // Interwały czasowe (format HH:MM:SS)
            "interval" -> {
                val parts = value.split(":")
                parts[0].toLong().hours +
                        parts[1].toLong().minutes +
                        parts[2].toLong().seconds
            }
            
            // Typy daty i czasu
            "date" -> LocalDate.parse(value)
            "timestamp" -> LocalDateTime.parse(value)
            "timestamptz" -> Instant.parse(value.replace(' ', 'T'))
            
            // Domyślnie wszystkie inne typy (text, varchar, char) jako String
            else -> value
        }
    }

    /**
     * Konwertuje typ enum PostgreSQL na odpowiedni enum Kotlin.
     *
     * Realizuje automatyczne mapowanie między konwencjami nazewniczymi PostgreSQL
     * (snake_case) a Kotlin (CamelCase). Proces konwersji:
     * 1. Konwertuje nazwę typu z snake_case na CamelCase (np. "reading_status" → "ReadingStatus")
     * 2. Wyszukuje pełną ścieżkę klasy enum w rejestrze typów
     * 3. Konwertuje wartość enum z snake_case na CamelCase (np. "reading" → "Reading")
     * 4. Tworzy instancję enum używając refleksji i metody valueOf()
     *
     * @param value Wartość enum z PostgreSQL (np. "reading", "not_started")
     * @param typeInfo Informacje o typie enum zawierające nazwę typu
     * @return Instancja enum lub null jeśli konwersja się nie powiedzie
     * @see Converters.snakeToCamelCase
     * @see TypeRegistry.findClassPath
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
     * Konwertuje tablicę PostgreSQL na List<Any?> Kotlin.
     *
     * Parsuje stringową reprezentację tablicy PostgreSQL (format: {val1,val2,val3})
     * i rekurencyjnie konwertuje każdy element tablicy na odpowiedni typ używając
     * głównej funkcji konwersji. Obsługuje zagnieżdżone tablice i tablice typów
     * kompozytowych.
     *
     * @param value Stringowa reprezentacja tablicy PostgreSQL (np. "{1,2,3}" lub "{a,b,c}")
     * @param typeInfo Informacje o typie tablicy zawierające typ elementu
     * @return Lista przekonwertowanych elementów jako List<Any?>
     * @throws IllegalStateException jeśli typ tablicy nie ma zdefiniowanego typu elementu
     */
    private fun convertArray(value: String, typeInfo: PostgresTypeInfo): List<Any?> {
        val elementType = typeInfo.elementType
            ?: throw IllegalStateException("Typ tablicowy ${typeInfo.typeName} nie ma zdefiniowanego typu elementu.")

        val elements = parsePostgresArray(value)

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
     * Konwertuje typ kompozytowy PostgreSQL na odpowiednią klasę Kotlin.
     *
     * Realizuje mapowanie typów kompozytowych PostgreSQL na data klasy Kotlin.
     * Proces konwersji:
     * 1. Parsuje stringową reprezentację typu kompozytowego (format: (val1,val2,val3))
     * 2. Waliduje zgodność liczby pól z liczbą atrybutów typu
     * 3. Rekurencyjnie konwertuje każde pole na odpowiedni typ
     * 4. Tworzy instancję klasy używając refleksji i pierwszego konstruktora
     *
     * @param value Stringowa reprezentacja typu kompozytowego (np. "(John,25,true)")
     * @param typeInfo Informacje o typie kompozytowym zawierające atrybuty i ich typy
     * @return Instancja klasy lub null jeśli konwersja się nie powiedzie
     * @throws IllegalStateException jeśli typ nie ma zdefiniowanych atrybutów
     * @throws IllegalArgumentException jeśli liczba pól nie zgadza się z liczbą atrybutów
     */
    private fun convertCompositeType(value: String, typeInfo: PostgresTypeInfo): Any? {
        val attributes = typeInfo.attributes
        if (attributes.isEmpty()) {
            throw IllegalStateException("Brak zdefiniowanych atrybutów dla typu kompozytowego ${typeInfo.typeName}")
        }

        val fields = parsePostgresComposite(value)

        if (fields.size != attributes.size) {
            println("Ostrzeżenie: liczba pól (${fields.size}) nie zgadza się z liczbą atrybutów (${attributes.size}) dla typu ${typeInfo.typeName}")
            throw IllegalArgumentException("Zła ilość pól dla typu: ${typeInfo.typeName}")
        }

        val attributeTypes = attributes.values.toList()

        // Przekształć każde pole na docelowy typ, wywołując rekurencyjnie główną funkcję konwersji
        val constructorArgs = fields.mapIndexed { index, fieldValue ->
            val attributeType = attributeTypes.getOrNull(index)
                ?: throw IllegalStateException("Brak typu dla atrybutu o indeksie $index w ${typeInfo.typeName}")
            convertToDomainType(fieldValue, attributeType)
        }.toTypedArray()

        return try {
            val className = Converters.snakeToCamelCase(typeInfo.typeName, true)
            val fullClassName = typeRegistry.findClassPath(className)
            val clazz = Class.forName(fullClassName)
            val constructor = clazz.constructors.first() // Używamy pierwszego konstruktora
            constructor.newInstance(*constructorArgs)
        } catch (e: Exception) {
            println(
                "Nie można utworzyć instancji obiektu dla typu kompozytowego: ${typeInfo.typeName} z wartością $value (próbowano: ${
                    typeRegistry.findClassPath(
                        Converters.snakeToCamelCase(typeInfo.typeName, true)
                    )
                })"
            )
            e.printStackTrace()
            null
        }
    }

    // =================================================================
    // === FUNKCJE PARSERA STRUKTUR POSTGRESQL ===
    // =================================================================

    /**
     * Parsuje stringową reprezentację tablicy PostgreSQL.
     *
     * @param pgArrayString Reprezentacja tablicy w formacie PostgreSQL (np. "{val1,val2,val3}")
     * @return Lista elementów tablicy jako String?
     */
    private fun parsePostgresArray(pgArrayString: String): List<String?> {
        return _parseNestedStructure(pgArrayString, '{', '}')
    }

    /**
     * Parsuje stringową reprezentację typu kompozytowego PostgreSQL.
     *
     * @param pgCompositeString Reprezentacja typu kompozytowego w formacie PostgreSQL (np. "(val1,val2,val3)")
     * @return Lista pól typu kompozytowego jako String?
     */
    private fun parsePostgresComposite(pgCompositeString: String): List<String?> {
        return _parseNestedStructure(pgCompositeString, '(', ')')
    }

    /**
     * Uniwersalny parser dla zagnieżdżonych struktur PostgreSQL.
     *
     * Zaawansowany parser obsługujący złożone struktury danych PostgreSQL.
     * Funkcjonalności:
     * - **Cudzysłowy i escapowanie**: Obsługuje wartości w cudzysłowach z escape'owanymi znakami
     * - **Zagnieżdżone struktury**: Tablice w tablicach, typy kompozytowe w tablicach
     * - **Wartości NULL**: Rozpoznaje wartości NULL (case-insensitive)
     * - **Poziomy zagnieżdżenia**: Śledzi poziomy nawiasów {} i () dla prawidłowego parsowania
     *
     * @param input Wejściowy string do sparsowania
     * @param startChar Znak początkowy struktury ('{' dla tablic, '(' dla typów kompozytowych)
     * @param endChar Znak końcowy struktury ('}' dla tablic, ')' dla typów kompozytowych)
     * @return Lista sparsowanych elementów jako String?
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
     * Usuwa escape'owanie z wartości PostgreSQL.
     *
     * Przetwarza surowe wartości z PostgreSQL, obsługując różne przypadki:
     * - **Wartości NULL**: Rozpoznaje "NULL" (case-insensitive) jako null
     * - **Cudzysłowy**: Usuwa zewnętrzne cudzysłowy i przetwarza escape'owane znaki
     * - **Escape'owanie**: Obsługuje "" → ", \" → ", \\ → \
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
