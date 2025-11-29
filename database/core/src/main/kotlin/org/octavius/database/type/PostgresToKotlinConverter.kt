package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.octavius.data.exception.ConversionException
import org.octavius.data.exception.ConversionExceptionMessage
import org.octavius.data.exception.TypeRegistryException
import org.octavius.data.exception.TypeRegistryExceptionMessage
import org.octavius.data.toDataObject

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
    }

    /**
     * Główna funkcja konwertująca, delegująca do specjalistycznych handlerów.
     *
     * Obsługuje wszystkie kategorie typów: STANDARD, ENUM, ARRAY, COMPOSITE i DYNAMIC.
     *
     * @param value Wartość z bazy danych jako `String` (może być `null`).
     * @param pgTypeName Nazwa typu w PostgreSQL (np. "int4", "my_enum", "dynamic_dto").
     * @return Przekonwertowana wartość lub `null` jeśli `value` było `null`.
     * @throws org.octavius.data.exception.TypeRegistryException jeśli typ jest nieznany.
     * @throws ConversionException jeśli konwersja się nie powiedzie.
     */
    fun convert(value: String?, pgTypeName: String): Any? {
        if (value == null) {
            logger.trace { "Converting null value for type: $pgTypeName" }
            return null
        }

        logger.trace { "Converting value '$value' from PostgreSQL type: $pgTypeName" }
        val category = typeRegistry.getCategory(pgTypeName)

        return when (category) {
            TypeCategory.STANDARD -> {
                logger.trace { "Converting standard value '$value' for type $pgTypeName" }
                convertStandardType(value, pgTypeName)
            }

            TypeCategory.ENUM -> {
                logger.trace { "Converting enum value '$value' for type $pgTypeName" }
                val def = typeRegistry.getEnumDefinition(pgTypeName)
                convertEnum(value, def)
            }

            TypeCategory.ARRAY -> {
                logger.trace { "Converting array value for type $pgTypeName" }
                val def = typeRegistry.getArrayDefinition(pgTypeName)
                convertArray(value, def)
            }

            TypeCategory.COMPOSITE -> {
                logger.trace { "Converting composite value for type $pgTypeName" }
                val def = typeRegistry.getCompositeDefinition(pgTypeName)
                convertCompositeType(value, def)
            }

            TypeCategory.DYNAMIC -> {
                logger.trace { "Converting dynamic DTO value for type $pgTypeName" }
                convertDynamicType(value)
            }
        }
    }

    /**
     * Deserializuje specjalny typ `dynamic_dto` na odpowiednią klasę Kotlina.
     *
     * @param value Surowa wartość z bazy w formacie kompozytu `("typeName", "jsonData")`.
     * @return Instancja odpowiedniej `data class` z adnotacją `@DynamicallyMappable`.
     */
    private fun convertDynamicType(value: String): Any? {

        val parts: List<String?> = parseNestedStructure(value)

        if (parts.size != 2) {
            throw TypeRegistryException(TypeRegistryExceptionMessage.WRONG_FIELD_NUMBER_IN_COMPOSITE, typeName = "dynamic_dto")
        }

        val typeName = parts[0]
        val jsonDataString = parts[1]

        if (typeName == null || jsonDataString == null) {
            throw ConversionException(ConversionExceptionMessage.INVALID_DYNAMIC_DTO_FORMAT, value = value)
        }

        // Użyj TypeRegistry do bezpiecznego znalezienia serializatora
        val serializer = typeRegistry.getDynamicSerializer(typeName)

        return try {
            Json.decodeFromString(serializer, jsonDataString)
        } catch (e: Exception) {
            throw ConversionException(ConversionExceptionMessage.JSON_DESERIALIZATION_FAILED, targetType = typeName, rowData = mapOf("json" to jsonDataString), cause = e)
        }
    }

    /**
     * Konwertuje standardowe typy PostgreSQL na odpowiednie typy Kotlina.
     *
     * Deleguje do `StandardTypeMappingRegistry`, które jest jedynym źródłem prawdy.
     *
     * @param value Wartość z bazy danych jako String.
     * @param pgTypeName Nazwa standardowego typu PostgreSQL.
     * @return Przekonwertowana wartość.
     * @throws ConversionException jeśli konwersja się nie powiedzie.
     */
    private fun convertStandardType(value: String, pgTypeName: String): Any? {
        // 1. Znajdź odpowiedni handler w centralnym rejestrze
        val handler = StandardTypeMappingRegistry.getHandler(pgTypeName)

        if (handler == null) {
            logger.warn { "No standard type handler found for PostgreSQL type '$pgTypeName'. Returning raw string value." }
            return value // Domyślne zachowanie: zwróć string, jeśli typ jest nieznany
        }

        // 2. Użyj funkcji 'fromString' z handlera do konwersji
        return try {
            handler.fromString(value)
        } catch (e: Exception) {
            throw ConversionException(
                messageEnum = ConversionExceptionMessage.VALUE_CONVERSION_FAILED,
                value = value,
                targetType = handler.kotlinClass.simpleName ?: pgTypeName,
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
     * @throws ConversionException jeśli konwersja się nie powiedzie.
     */
    private fun convertEnum(value: String, typeInfo: PgEnumDefinition): Any? {

        return typeInfo.valueToEnumMap[value]
            ?: throw ConversionException(
                messageEnum = ConversionExceptionMessage.ENUM_CONVERSION_FAILED,
                value = value,
                targetType = typeInfo.kClass.simpleName
            )
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
     * @throws ConversionException jeśli parsowanie się nie powiedzie.
     */
    private fun convertArray(value: String, typeInfo: PgArrayDefinition): List<Any?> {

        logger.trace { "Parsing PostgreSQL array with element type: ${typeInfo.elementTypeName}" }

        val elements: List<String?> = parseNestedStructure(value)

        logger.trace { "Parsed ${elements.size} array elements" }

        // Rekurencyjnie konwertujemy każdy element tablicy używając głównej funkcji konwersji
        return elements.map { elementValue ->
            // Sprawdzamy, czy string reprezentujący element SAM jest tablicą.
            val isNestedArray = elementValue?.startsWith('{') ?: false

            // Jeśli to zagnieżdżona tablica, rekurencyjnie wywołujemy konwersję
            // dla CAŁEGO typu tablicowego (np. "_text"), a nie dla jego elementu ("text").
            // W przeciwnym razie, kontynuujemy standardową logikę z elementType.
            val typeNameToUse = if (isNestedArray) typeInfo.typeName else typeInfo.elementTypeName

            convert(elementValue, typeNameToUse)
        }
    }

    /**
     * Konwertuje typ kompozytowy PostgreSQL na `data class` Kotlina.
     * Wykorzystuje cache dla KClass i deleguje do `toDataObject` (które samo ma cache).
     */
    private fun convertCompositeType(value: String, typeInfo:  PgCompositeDefinition): Any? {

        logger.trace { "Converting composite type ${typeInfo.typeName} to class: ${typeInfo.kClass.qualifiedName}" }

        // 1. Parsowanie stringa na listę surowych wartości
        val fieldValues: List<String?> = parseNestedStructure(value)


        val dbAttributes = typeInfo.attributes.toList()

        if (fieldValues.size != dbAttributes.size) {
            val ex = TypeRegistryException(TypeRegistryExceptionMessage.WRONG_FIELD_NUMBER_IN_COMPOSITE, typeName = typeInfo.typeName)
            logger.error(ex) { ex }
            throw ex
        }

        logger.trace { "Converting ${dbAttributes.size} composite fields" }
        val constructorArgsMap = dbAttributes.mapIndexed { index, (dbAttributeName, dbAttributeType) ->
            val convertedValue = convert(fieldValues[index], dbAttributeType)
            dbAttributeName to convertedValue
        }.toMap()

        return try {
            val result = constructorArgsMap.toDataObject(typeInfo.kClass)
            logger.trace { "Successfully created instance of ${typeInfo.kClass.simpleName}" }
            result
        } catch (e: Exception) { // To zawsze powinien być ConversionException
            logger.error(e) { e }
            throw e
        }
    }

    // =================================================================
    // --- PARSER STRUKTUR POSTGRESQL ---
    // =================================================================

    /**
     * Uniwersalny parser dla zagnieżdżonych struktur (tablic i kompozytów).
     * Obsługuje cudzysłowy, escapowanie, wartości `NULL` i zagnieżdżenia.
     */
    private fun parseNestedStructure(input: String): List<String?> {

        val content = input.substring(1, input.length - 1)
        if (content.isEmpty()) return emptyList()

        val elements = mutableListOf<String?>()
        var currentElementStart = 0
        var inQuotes = false
        var nestingLevel = 0 // Poziom zagnieżdżenia nawiasów

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
                    '{', '(' -> nestingLevel++
                    '}', ')' -> nestingLevel--
                    ',' -> {
                        // Przecinek na najwyższym poziomie = separator elementów
                        if (nestingLevel == 0) {
                            elements.add(unescapeValue(content.substring(currentElementStart, i)))
                            currentElementStart = i + 1
                        }
                    }
                }
            }
            i++
        }
        elements.add(unescapeValue(content.substring(currentElementStart)))
        return elements
    }

    /**
     * Przetwarza surową wartość, usuwając cudzysłowy i escapowanie.
     * Poprawnie interpretuje `NULL` (jawne `NULL` lub pusty, niecytowany string)
     * oraz pusty string (reprezentowany jako `""`).
     */
    private fun unescapeValue(raw: String): String? {

        // 1. Sprawdzamy, czy wartość jest w cudzysłowach.
        if (raw.startsWith('"') && raw.endsWith('"')) {
            // Jeśli tak, to jest to jawny string. Nawet jeśli pusty (""), to jest to pusty string, a nie NULL.
            return raw.substring(1, raw.length - 1)
                .replace("\"\"", "\"") // PostgreSQL escapuje cudzysłów przez podwojenie go
                .replace("\\\"", "\"") // Obsługa standardowego escape'owania
                .replace("\\\\", "\\")
        }

        // 2. Jeśli wartość NIE jest w cudzysłowach.
        // Pusty, nieopakowany w cudzysłowy ciąg znaków w kompozycie oznacza NULL.
        if (raw.isEmpty() || raw.equals("NULL", ignoreCase = true)) {
            return null
        }

        // 3. W każdym innym przypadku jest to zwykła, nieopakowana w cudzysłowy wartość.
        return raw
    }
}