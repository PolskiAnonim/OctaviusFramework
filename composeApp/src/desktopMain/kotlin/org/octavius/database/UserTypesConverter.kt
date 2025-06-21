package org.octavius.database

import org.octavius.util.Converters
import org.postgresql.jdbc.PgArray
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import java.text.ParseException

class UserTypesConverter(private val jdbcTemplate: JdbcTemplate) {
    // Przechowuje mapowanie nazw typów PostgreSQL na informacje o typach
    private val postgresTypeMap = mutableMapOf<String, PostgresTypeInfo>()

    // Pomocnicze klasy do mapowania wyników zapytań
    private data class EnumTypeInfo(val typeName: String, val value: String)
    private data class CompositeAttributeInfo(
        val typeName: String,
        val attributeName: String,
        val attributeType: String
    )

    // Inicjalizacja konwertera typów
    fun initialize() {
        loadStandardTypes()
        loadEnumTypes()
        loadCompositeTypes()
        loadArrayTypes()
    }

    // Ładowanie definicji standardowych typów
    private fun loadStandardTypes() {
        // Mapowanie standardowych typów PostgreSQL
        val typeNames = listOf(
            // Typy numeryczne
            "serial",
            "int4",
            "int8",
            "int2",
            "float4",
            "float8",
            "numeric",
            // Typy tekstowe
            "text",
            "varchar",
            "char",
            // Inne
            "bool",
            "date",
            "timestamp",
            "timestamptz",
            "json",
            "jsonb",
            "uuid",
            // Interval
            "interval"
        )

        typeNames.forEach { typeName ->
            postgresTypeMap[typeName] = PostgresTypeInfo(typeName, TypeCategory.STANDARD)
        }
    }

    // Ładowanie definicji typów wyliczeniowych (enum)
    private fun loadEnumTypes() {
        val query = """
            SELECT 
                t.typname AS enum_type,
                e.enumlabel AS enum_value
            FROM 
                pg_type t
                JOIN pg_enum e ON t.oid = e.enumtypid
                JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
            WHERE 
                n.nspname = ANY(ARRAY['public','asian_media', 'games'])
            ORDER BY 
                t.typname, e.enumsortorder
        """

        val enumValues = jdbcTemplate.query(query) { rs, _ ->
            EnumTypeInfo(rs.getString("enum_type"), rs.getString("enum_value"))
        }

        // Grupowanie wartości enum według nazwy typu
        enumValues.groupBy { it.typeName }.forEach { (typeName, values) ->
            postgresTypeMap[typeName] = PostgresTypeInfo(
                typeName = typeName,
                typeCategory = TypeCategory.ENUM,
                enumValues = values.map { it.value }
            )
        }
    }

    // Ładowanie typów złożonych (composite)
    private fun loadCompositeTypes() {
        val query = """
            SELECT 
                t.typname AS type_name,
                a.attname AS attr_name,
                at.typname AS attr_type
            FROM 
                pg_type t
                JOIN pg_class c ON t.typrelid = c.oid
                JOIN pg_attribute a ON a.attrelid = c.oid
                JOIN pg_type at ON a.atttypid = at.oid
                JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
            WHERE 
                t.typtype = 'c'
                AND a.attnum > 0
                AND NOT a.attisdropped
                AND n.nspname = ANY(ARRAY['public','asian_media', 'games'])
            ORDER BY 
                t.typname, a.attnum
        """

        val attributes = jdbcTemplate.query(query) { rs, _ ->
            CompositeAttributeInfo(
                rs.getString("type_name"),
                rs.getString("attr_name"),
                rs.getString("attr_type")
            )
        }

        // Grupowanie atrybutów według nazwy typu
        attributes.groupBy { it.typeName }.forEach { (typeName, attrs) ->
            postgresTypeMap[typeName] = PostgresTypeInfo(
                typeName = typeName,
                typeCategory = TypeCategory.COMPOSITE,
                attributes = attrs.associate { it.attributeName to it.attributeType }
            )
        }
    }

    // Ładowanie definicji typów tablicowych
    private fun loadArrayTypes() {
        val keys = postgresTypeMap.keys.toList()
        keys.forEach {
            postgresTypeMap["_$it"] = PostgresTypeInfo("_$it", TypeCategory.ARRAY, elementType = it)
        }
    }


    /**
     * Główna funkcja konwertująca wartości PostgreSQL na Kotlina. Przyjmuje wartość jako String.
     */
    fun convertToDomainType(value: String?, pgTypeName: String): Any? {
        if (value == null) return null

        val typeInfo = postgresTypeMap[pgTypeName]
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
        // Znajdź odpowiednią klasę enum w Kotlinie
        val enumClassName = "org.octavius.domain.${Converters.snakeToCamelCase(typeInfo.typeName, true)}"

        return try {
            val enumClass = Class.forName(enumClassName)
            val enumValueName = Converters.snakeToCamelCase(value, true)
            val method = enumClass.getMethod("valueOf", String::class.java)
            method.invoke(null, enumValueName)
        } catch (e: Exception) {
            println("Nie można przekonwertować wartości enum: $value dla typu ${typeInfo.typeName}")
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
            val className = "org.octavius.domain.${Converters.snakeToCamelCase(typeInfo.typeName, true)}"
            val clazz = Class.forName(className)
            val constructor = clazz.constructors.first() // Zakładamy, że jest jeden konstruktor
            constructor.newInstance(*constructorArgs)
        } catch (e: Exception) {
            println("Nie można utworzyć instancji obiektu dla typu kompozytowego: ${typeInfo.typeName} z wartością $value")
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

// Kategorie typów PostgreSQL
enum class TypeCategory {
    ENUM,
    ARRAY,
    COMPOSITE,
    STANDARD
}

// Klasa przechowująca informacje o typie PostgreSQL
data class PostgresTypeInfo(
    val typeName: String,
    val typeCategory: TypeCategory,
    val enumValues: List<String> = emptyList(),
    val elementType: String? = null,
    val attributes: Map<String, String> = emptyMap()
)