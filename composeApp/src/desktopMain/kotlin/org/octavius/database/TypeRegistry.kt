package org.octavius.database

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class TypeRegistry(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    // Przechowuje mapowanie nazw typów PostgreSQL na informacje o typach
    private val postgresTypeMap = mutableMapOf<String, PostgresTypeInfo>()
    
    // Mapa mapująca nazwy klas na ich pełne ścieżki
    private val classPathMap = mutableMapOf<String, String>()

    // Pomocnicze klasy do mapowania wyników zapytań
    private data class EnumTypeInfo(val typeName: String, val value: String)
    private data class CompositeAttributeInfo(
        val typeName: String,
        val attributeName: String,
        val attributeType: String
    )

    init {
        scanDomainClasses()
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

    // Skanuje wszystkie klasy w pakiecie domain i mapuje je na ścieżki
    private fun scanDomainClasses() {
        val baseDomainPackage = "org.octavius.domain"
        try {
            // Używamy ClassLoader do znalezienia wszystkich klas w pakiecie domain
            val classLoader = Thread.currentThread().contextClassLoader
            val packagePath = baseDomainPackage.replace('.', '/')
            val resources = classLoader.getResources(packagePath)
            
            while (resources.hasMoreElements()) {
                val resource = resources.nextElement()
                if (resource.protocol == "file") {
                    val file = java.io.File(resource.toURI())
                    scanDirectory(file, baseDomainPackage)
                }
            }
        } catch (e: Exception) {
            println("Błąd podczas skanowania klas domain: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun scanDirectory(directory: java.io.File, packageName: String) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    scanDirectory(file, "$packageName.${file.name}")
                }
                file.name.endsWith(".class") -> {
                    val className = file.name.removeSuffix(".class")
                    classPathMap[className] = "$packageName.$className"
                }
            }
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

        val enumValues = namedParameterJdbcTemplate.query(query, emptyMap<String, Any>()) { rs, _ ->
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

        val attributes = namedParameterJdbcTemplate.query(query, emptyMap<String, Any>()) { rs, _ ->
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

    // Publiczne metody dostępu do informacji o typach
    fun getTypeInfo(pgTypeName: String): PostgresTypeInfo? {
        return postgresTypeMap[pgTypeName]
    }

    fun findClassPath(className: String): String? {
        return classPathMap[className] ?: "org.octavius.domain.$className"
    }

    fun getAllRegisteredTypes(): Map<String, PostgresTypeInfo> {
        return postgresTypeMap.toMap()
    }

    fun getClassMappings(): Map<String, String> {
        return classPathMap.toMap()
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