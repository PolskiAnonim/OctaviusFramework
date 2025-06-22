package org.octavius.database

import org.octavius.util.Converters
import java.text.ParseException

class UserTypesConverter(private val typeRegistry: TypeRegistry) {

    /**
     * Główna funkcja konwertująca wartości PostgreSQL na Kotlina. Przyjmuje wartość jako String.
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
     * Konwertuje standardowe typy z ich stringowej reprezentacji.
     */
    private fun convertStandardType(value: String, pgTypeName: String): Any? {
        // Zawsze operujemy na stringu
        return when (pgTypeName) {
            "int4", "serial", "int2" -> value.toIntOrNull()
            "int8" -> value.toLongOrNull()
            "float4" -> value.toFloatOrNull()
            "float8", "numeric" -> value.toDoubleOrNull()
            "bool" -> value.toBooleanStrictOrNull()
            "json", "jsonb", "uuid", "interval", "date", "timestamp", "timestamptz" -> value // do aktualizacji
            else -> value // Domyślnie zwracamy string
        }
    }

    /**
     * Konwersja typu enum
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
     * Konwertuje tablicę na podstawie jej stringowej reprezentacji.
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
     * Konwertuje typ kompozytowy na podstawie jego stringowej reprezentacji.
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

    private fun parsePostgresArray(pgArrayString: String): List<String?> {
        return _parseNestedStructure(pgArrayString, '{', '}')
    }

    private fun parsePostgresComposite(pgCompositeString: String): List<String?> {
        return _parseNestedStructure(pgCompositeString, '(', ')')
    }

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