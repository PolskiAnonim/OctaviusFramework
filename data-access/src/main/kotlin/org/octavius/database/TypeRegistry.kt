package org.octavius.database

import io.github.classgraph.ClassGraph
import org.octavius.data.contract.EnumCaseConvention
import org.octavius.data.contract.PgType
import org.octavius.util.Converters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.reflect.KClass

/**
 * Skanuje bazę danych i klasy domenowe, tworząc mapowanie między typami PostgreSQL a klasami Kotlina.
 *
 * Inicjalizacja polega na odpytaniu katalogów systemowych bazy danych (`pg_type`, `pg_enum`)
 * oraz przeskanowaniu classpath w poszukiwaniu klas domenowych, aby umożliwić
 * automatyczną konwersję typów.
 *
 * @param namedParameterJdbcTemplate Template JDBC do odpytywania bazy danych.
 */
class TypeRegistry(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    // Przechowuje mapowanie nazw typów PostgreSQL na informacje o typach
    private val postgresTypeMap = mutableMapOf<String, PostgresTypeInfo>()

    // Mapa: Prosta nazwa klasy -> Pełna ścieżka klasy
    private val classSimpleNameToFullPathMap = mutableMapOf<String, String>()

    // Mapa: Pełna ścieżka klasy -> Nazwa typu w PostgreSQL
    private val classFullPathToPgTypeNameMap = mutableMapOf<String, String>()

    // Mapa: Nazwa typu w PostgreSQL -> Pełna ścieżka klasy
    private val pgTypeNameToClassFullPathMap = mutableMapOf<String, String>()

    /**
     * Klasa pomocnicza do mapowania informacji o typach enum z bazy danych.
     * 
     * @param typeName Nazwa typu enum w PostgreSQL
     * @param value Pojedyncza wartość enum
     */
    private data class EnumTypeInfo(val typeName: String, val value: String)
    
    /**
     * Klasa pomocnicza do mapowania atrybutów typów kompozytowych z bazy danych.
     * 
     * @param typeName Nazwa typu kompozytowego
     * @param attributeName Nazwa atrybutu
     * @param attributeType Typ atrybutu
     */
    private data class CompositeAttributeInfo(
        val typeName: String,
        val attributeName: String,
        val attributeType: String
    )

    private val pgNameToEnumConventionMap = mutableMapOf<String, EnumCaseConvention>()

    init {
        scanDomainClasses()
        loadStandardTypes()
        loadEnumTypes()
        loadCompositeTypes()
        loadArrayTypes()
    }

    /**
     * Ładuje definicje standardowych typów PostgreSQL.
     * 
     * Rejestruje mapowanie dla podstawowych typów takich jak:
     * - Typy numeryczne (serial, int4, int8, int2, float4, float8, numeric)
     * - Typy tekstowe (text, varchar, char)
     * - Inne (bool, date, timestamp, json, jsonb, uuid, interval)
     */
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

    /**
     * Skanuje wszystkie klasy w pakiecie domenowym i mapuje je na pełne ścieżki.
     * 
     * Przeszukuje pakiet zdefiniowany w [DatabaseConfig.baseDomainPackage] używając ClassLoader
     * i tworzy mapowanie nazw klas na ich pełne ścieżki pakietowe.
     * 
     * @throws Exception Jeśli wystąpi błąd podczas skanowania (błąd jest logowany, ale nie przerywa działania)
     */
    private fun scanDomainClasses() {
        val baseDomainPackage = DatabaseConfig.baseDomainPackage
        val annotationName = "org.octavius.data.contract.PgType"

        try {
            // Użycie ClassGraph jest o wiele prostsze i bardziej niezawodne
            ClassGraph()
                .enableAllInfo() // Potrzebujemy informacji o adnotacjach
                .acceptPackages(baseDomainPackage) // Ogranicz skanowanie dla wydajności
                .scan().use { scanResult -> // .use { } automatycznie zamyka zasoby

                    scanResult.getClassesWithAnnotation(PgType::class.java).forEach { classInfo ->
                        val simpleName = classInfo.simpleName
                        val fullPath = classInfo.name

                        // Zapełnij mapę prostych nazw
                        classSimpleNameToFullPathMap[simpleName] = fullPath

                        // Odczytaj adnotację, żeby poznać nazwę w PG
                        val annotationInfo = classInfo.getAnnotationInfo(PgType::class.java)
                        val pgTypeNameFromAnnotation = annotationInfo.parameterValues.getValue("name") as String

                        val pgTypeName = pgTypeNameFromAnnotation.ifBlank {
                            Converters.toSnakeCase(simpleName) // Użyj konwencji
                        }

                        // Zapełnij nowe, dwukierunkowe mapy
                        classFullPathToPgTypeNameMap[fullPath] = pgTypeName
                        pgTypeNameToClassFullPathMap[pgTypeName] = fullPath

                        if (classInfo.isEnum) {
                            val conventionEnumValue = annotationInfo.parameterValues.getValue("enumConvention") as? io.github.classgraph.AnnotationEnumValue
                            if (conventionEnumValue != null) {
                                pgNameToEnumConventionMap[pgTypeName] = conventionEnumValue.loadClassAndReturnEnumValue() as EnumCaseConvention
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            println("Błąd podczas skanowania klas z adnotacją @PgType: ${e.message}")
            e.printStackTrace()
        }
    }


    /**
     * Ładuje definicje typów wyliczeniowych (enum) z bazy danych.
     * 
     * Wykonuje zapytanie do katalogu systemowego PostgreSQL (pg_type, pg_enum)
     * aby pobrać wszystkie typy enum z określonych schematów wraz z ich wartościami.
     * 
     * Typy są przechowywane w [postgresTypeMap] z kategorią [TypeCategory.ENUM].
     */
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
                n.nspname = ANY(:schemas)
            ORDER BY 
                t.typname, e.enumsortorder
        """

        val params = mapOf("schemas" to DatabaseConfig.dbSchemas.toTypedArray())
        val enumValues = namedParameterJdbcTemplate.query(query, params) { rs, _ ->
            EnumTypeInfo(rs.getString("enum_type"), rs.getString("enum_value"))
        }

        // Grupowanie wartości enum według nazwy typu
        enumValues.groupBy { it.typeName }.forEach { (typeName, values) ->

            val convention = pgNameToEnumConventionMap[typeName]
                ?: EnumCaseConvention.SNAKE_CASE_UPPER

            postgresTypeMap[typeName] = PostgresTypeInfo(
                typeName = typeName,
                typeCategory = TypeCategory.ENUM,
                enumValues = values.map { it.value },
                enumConvention = convention
            )
        }
    }

    /**
     * Ładuje typy złożone (composite) z bazy danych.
     * 
     * Wykonuje zapytanie do katalogu systemowego PostgreSQL aby pobrać
     * definicje typów kompozytowych wraz z ich atrybutami i typami atrybutów.
     * 
     * Typy są przechowywane w [postgresTypeMap] z kategorią [TypeCategory.COMPOSITE].
     */
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
                AND n.nspname = ANY(:schemas)
            ORDER BY 
                t.typname, a.attnum
        """

        val params = mapOf("schemas" to DatabaseConfig.dbSchemas.toTypedArray())
        val attributes = namedParameterJdbcTemplate.query(query, params) { rs, _ ->
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

    /**
     * Ładuje definicje typów tablicowych.
     * 
     * Automatycznie generuje typy tablicowe dla wszystkich już zarejestrowanych typów
     * poprzez dodanie prefiksu "_" do nazwy typu (konwencja PostgreSQL).
     * 
     * Typy tablicowe są przechowywane z kategorią [TypeCategory.ARRAY].
     */
    private fun loadArrayTypes() {
        val keys = postgresTypeMap.keys.toList()
        keys.forEach {
            postgresTypeMap["_$it"] = PostgresTypeInfo("_$it", TypeCategory.ARRAY, elementType = it)
        }
    }

    /**
     * Zwraca informacje o typie PostgreSQL.
     * 
     * @param pgTypeName Nazwa typu w PostgreSQL
     * @return Informacje o typie lub null jeśli typ nie został znaleziony
     */
    fun getTypeInfo(pgTypeName: String): PostgresTypeInfo? {
        return postgresTypeMap[pgTypeName]
    }

    // Nowe, publiczne metody dostępu
    fun getPgTypeNameForClass(clazz: KClass<*>): String? {
        return classFullPathToPgTypeNameMap[clazz.qualifiedName]
    }

    fun getClassFullPathForPgTypeName(pgTypeName: String): String? {
        return pgTypeNameToClassFullPathMap[pgTypeName]
    }

    /**
     * Zwraca wszystkie zarejestrowane typy PostgreSQL.
     * 
     * @return Niemodyfikowalna mapa nazw typów na informacje o typach
     */
    fun getAllRegisteredTypes(): Map<String, PostgresTypeInfo> {
        return postgresTypeMap.toMap()
    }
}

/**
 * Kategorie typów PostgreSQL obsługiwane przez system.
 */
enum class TypeCategory {
    /** Typ wyliczeniowy (CREATE TYPE ... AS ENUM) */
    ENUM,
    /** Typ tablicowy (prefikś "_" w nazwie typu) */
    ARRAY,
    /** Typ kompozytowy (CREATE TYPE ... AS) */
    COMPOSITE,
    /** Standardowy typ PostgreSQL (int4, text, bool, itp.) */
    STANDARD
}

/**
 * Klasa przechowująca informacje o typie PostgreSQL.
 * 
 * @param typeName Nazwa typu w PostgreSQL
 * @param typeCategory Kategoria typu
 * @param enumValues Lista wartości dla typów enum (pusta dla innych typów)
 * @param enumConvention Konwencja konwersji nazw enum
 * @param elementType Typ elementu dla typów tablicowych (null dla innych typów)
 * @param attributes Mapa atrybutów dla typów kompozytowych (pusta dla innych typów)
 */
data class PostgresTypeInfo(
    val typeName: String,
    val typeCategory: TypeCategory,
    val enumValues: List<String> = emptyList(),
    val enumConvention: EnumCaseConvention = EnumCaseConvention.SNAKE_CASE_LOWER,
    val elementType: String? = null,
    val attributes: Map<String, String> = emptyMap()
)