package org.octavius.novels.database

import org.octavius.novels.util.Converters
import org.postgresql.jdbc.PgArray
import org.springframework.jdbc.core.JdbcTemplate

class UserTypesConverter(private val jdbcTemplate: JdbcTemplate) {
    // Przechowuje mapowanie nazw typów PostgreSQL na informacje o typach
    private val postgresTypeMap = mutableMapOf<String, PostgresTypeInfo>()

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
            "uuid"
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


    // Konwersja wartości PostgreSQL na typ Kotlina
    fun convertToDomainType(value: Any?, pgTypeName: String): Any? {
        if (value == null) return null

        // Sprawdzamy, czy mamy informacje o typie
        val typeInfo = postgresTypeMap[pgTypeName]

        return when (typeInfo?.typeCategory) {
            // Konwersja enum
            TypeCategory.ENUM -> {
                convertEnum(value.toString(), typeInfo)
            }
            // Konwersja tablicy
            TypeCategory.ARRAY -> {
                convertArray(value, typeInfo)
            }
            // Konwersja typu kompozytowego
            TypeCategory.COMPOSITE -> {
                convertCompositeType(value, typeInfo)
            }
            // Domyślne typy PostgreSQL
            TypeCategory.STANDARD -> {
                convertStandardType(value, pgTypeName)
            }

            else -> {
                throw IllegalArgumentException("Unsupported type $pgTypeName")
            }
        }
    }

    // Konwersja standardowych typów PostgreSQL
    private fun convertStandardType(value: Any?, pgTypeName: String): Any? {
        return when (pgTypeName) {
            "int4", "int8", "int2" -> if (value is Number) value.toInt() else value
            "float4", "float8" -> if (value is Number) value.toDouble() else value
            "numeric" -> if (value is Number) value.toDouble() else value
            // Można dodać więcej typów w miarę potrzeby
            else -> value
        }
    }

    // Konwersja typu enum
    private fun convertEnum(value: String, typeInfo: PostgresTypeInfo): Any? {
        // Znajdź odpowiednią klasę enum w Kotlinie
        val enumClassName = "org.octavius.novels.domain.${Converters.snakeToCamelCase(typeInfo.typeName, true)}"

        return try {
            val enumClass = Class.forName(enumClassName)
            val enumValue = Converters.snakeToCamelCase(value, true)
            val method = enumClass.getMethod("valueOf", String::class.java)
            method.invoke(null, enumValue)
        } catch (e: Exception) {
            println("Nie można przekonwertować wartości enum: $value dla typu ${typeInfo.typeName}")
            e.printStackTrace()
            null
        }
    }

    // Konwersja tablicy
    private fun convertArray(value: Any, typeInfo: PostgresTypeInfo?): Any? {
        // Określ typ elementów tablicy
        val elementType = typeInfo?.elementType as String
        val javaArray = (value as PgArray).array
        return if (javaArray is Array<*>) {
            javaArray.map { convertToDomainType(it, elementType) }
        } else {
            // Konwersja innych typów tablic (np. tablice prymitywów)
            when (javaArray) {
                is IntArray -> javaArray.toList()
                is LongArray -> javaArray.toList()
                is DoubleArray -> javaArray.toList()
                is BooleanArray -> javaArray.toList()
                else -> {
                    // Próba konwersji do listy
                    try {
                        val arrayAsList = javaArray.javaClass.getMethod("toList").invoke(javaArray) as? List<*>
                        arrayAsList?.map { convertToDomainType(it, elementType) } ?: listOf(javaArray)
                    } catch (e: Exception) {
                        println("Nie można przekonwertować tablicy: $javaArray")
                        null
                    }
                }
            }
        }
    }

    // Konwersja typu kompozytowego
    private fun convertCompositeType(value: Any?, typeInfo: PostgresTypeInfo): Any? {
        // Ta implementacja będzie bardziej złożona i zależy od konkretnych potrzeb
        // Można użyć refleksji do utworzenia instancji odpowiedniej klasy w Kotlinie
        return value // Uproszczona implementacja
    }

    // Funkcja pomocnicza do sprawdzenia, czy dany typ jest typem enum
    fun isEnumType(pgTypeName: String): Boolean {
        return postgresTypeMap[pgTypeName]?.typeCategory == TypeCategory.ENUM
    }

    // Funkcja pomocnicza do sprawdzenia, czy dany typ jest tablicą
    fun isArrayType(pgTypeName: String): Boolean {
        return postgresTypeMap[pgTypeName]?.typeCategory == TypeCategory.ARRAY
    }

    // Zwraca dostępne wartości enum dla danego typu
    fun getEnumValues(pgTypeName: String): List<String> {
        return postgresTypeMap[pgTypeName]?.enumValues ?: emptyList()
    }

    // Pomocnicze klasy do mapowania wyników zapytań
    private data class EnumTypeInfo(val typeName: String, val value: String)
    private data class CompositeAttributeInfo(
        val typeName: String,
        val attributeName: String,
        val attributeType: String
    )
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