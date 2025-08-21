package org.octavius.database.type

import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.EnumCaseConvention
import org.octavius.data.contract.PgType
import org.octavius.database.DatabaseConfig
import org.octavius.util.Converters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Buduje instancję [TypeRegistry] poprzez skanowanie bazy danych i klas domenowych.
 *
 * Ta klasa jest odpowiedzialna za łączenie się z bazą danych,
 * wykonywanie zapytań do katalogów systemowych PostgreSQL oraz skanowanie classpath
 * w poszukiwaniu adnotacji.
 *
 * @param namedParameterJdbcTemplate Template JDBC do odpytywania bazy danych.
 */
class TypeRegistryLoader(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    private val logger = KotlinLogging.logger {}
    // Klasy pomocnicze
    private data class EnumTypeInfo(val typeName: String, val value: String)
    private data class CompositeAttributeInfo(val typeName: String, val attributeName: String, val attributeType: String)

    private data class DomainTypeInfo(val domainName: String, val baseTypeName: String)

    // Klasa pomocnicza do przechowywania zmapowanych informacji o klasach Kotlina.
    private data class KotlinPgTypeMapping(
        val classFullPath: String,
        val pgTypeName: String,
        val enumConvention: EnumCaseConvention?
    )

    /**
     * Główna metoda, która wykonuje wszystkie kroki i zwraca gotowy,
     * w pełni skonfigurowany [TypeRegistry].
     */
    fun load(): TypeRegistry {
        try {
            logger.info { "Starting TypeRegistry loading..." }
            val kotlinTypeMappings = scanDomainClasses()
            val postgresTypeMap = buildPostgresTypeMap(kotlinTypeMappings)
            val (classToPgMap, pgToClassMap) = buildBidirectionalClassMaps(kotlinTypeMappings)

            logger.info { "TypeRegistry loaded successfully. Found ${postgresTypeMap.size} total PG types." }
            logger.debug { "Kotlin class mappings found: ${kotlinTypeMappings.size}" }

            return TypeRegistry(
                postgresTypeMap = postgresTypeMap,
                classFullPathToPgTypeNameMap = classToPgMap,
                pgTypeNameToClassFullPathMap = pgToClassMap
            )
        } catch (e: Exception) {
            logger.error(e) { "FATAL: Failed to load TypeRegistry. Application state is inconsistent!" }
            throw IllegalStateException("Failed to initialize TypeRegistry", e)
        }
    }

    private fun buildPostgresTypeMap(kotlinMappings: List<KotlinPgTypeMapping>): Map<String, PostgresTypeInfo> {
        return buildMap {
            val pgNameToEnumConventionMap = kotlinMappings
                .filter { it.enumConvention != null }
                .associate { it.pgTypeName to it.enumConvention!! }

            putAll(loadStandardTypes())
            putAll(loadEnumTypes(pgNameToEnumConventionMap))
            putAll(loadCompositeTypes())
            putAll(loadDomainTypes())

            // Array types muszą być dodane na końcu, na podstawie już istniejących typów
            val existingKeys = keys.toList()
            existingKeys.forEach { elementType ->
                val arrayTypeName = "_$elementType"
                put(arrayTypeName, PostgresTypeInfo(arrayTypeName, TypeCategory.ARRAY, elementType = elementType))
            }
        }
    }

    private fun buildBidirectionalClassMaps(mappings: List<KotlinPgTypeMapping>): Pair<Map<String, String>, Map<String, String>> {
        val classToPg = mappings.associate { it.classFullPath to it.pgTypeName }
        val pgToClass = mappings.associate { it.pgTypeName to it.classFullPath }
        return classToPg to pgToClass
    }

    private fun scanDomainClasses(): List<KotlinPgTypeMapping> {
        val mappings = mutableListOf<KotlinPgTypeMapping>()

        try {
            ClassGraph()
                .enableAllInfo()
                .scan().use { scanResult ->
                    scanResult.getClassesWithAnnotation(PgType::class.java).forEach { classInfo ->
                        val annotationInfo = classInfo.getAnnotationInfo(PgType::class.java)
                        val pgTypeNameFromAnnotation = annotationInfo.parameterValues.getValue("name") as String
                        val pgTypeName = pgTypeNameFromAnnotation.ifBlank { Converters.toSnakeCase(classInfo.simpleName) }

                        var convention: EnumCaseConvention? = null
                        if (classInfo.isEnum) {
                            val conventionEnumValue = annotationInfo.parameterValues.getValue("enumConvention") as? io.github.classgraph.AnnotationEnumValue
                            convention = conventionEnumValue?.loadClassAndReturnEnumValue() as? EnumCaseConvention
                        }

                        mappings.add(KotlinPgTypeMapping(classInfo.name, pgTypeName, convention))
                    }
                }
        } catch (e: Exception) {
            println("Błąd podczas skanowania klas z adnotacją @PgType: ${e.message}")
            e.printStackTrace()
        }
        return mappings
    }

    private fun loadStandardTypes(): Map<String, PostgresTypeInfo> {
// Mapowanie standardowych typów PostgreSQL
        return listOf(
            // Typy stałoprzecinkowe
            "serial", "int4", "int8", "int2",
            // Typy zmiennoprzecinkowe
            "float4", "float8", "numeric",
            // Typy tekstowe
            "text", "varchar", "char",
            // Data i czas
            "date", "timestamp", "timestamptz",
            // Json
            "json",
            "jsonb",
            // Inne
            "bool",
            "uuid",
            "interval"
        ).associateWith { typeName ->
            PostgresTypeInfo(typeName, TypeCategory.STANDARD)
        }
    }

    private fun loadEnumTypes(pgNameToEnumConventionMap: Map<String, EnumCaseConvention>): Map<String, PostgresTypeInfo> {
        val params = mapOf("schemas" to DatabaseConfig.dbSchemas.toTypedArray())
        val enumValues = namedParameterJdbcTemplate.query(SQL_QUERY_ENUM_TYPES, params) { rs, _ ->
            EnumTypeInfo(rs.getString("enum_type"), rs.getString("enum_value"))
        }

        return enumValues.groupBy { it.typeName }.mapValues { (typeName, values) ->
            PostgresTypeInfo(
                typeName = typeName,
                typeCategory = TypeCategory.ENUM,
                enumValues = values.map { it.value },
                enumConvention = pgNameToEnumConventionMap[typeName] ?: EnumCaseConvention.SNAKE_CASE_UPPER
            )
        }
    }

    private fun loadCompositeTypes(): Map<String, PostgresTypeInfo> {
        val params = mapOf("schemas" to DatabaseConfig.dbSchemas.toTypedArray())
        val attributes = namedParameterJdbcTemplate.query(SQL_QUERY_COMPOSITE_TYPES, params) { rs, _ ->
            CompositeAttributeInfo(
                rs.getString("type_name"),
                rs.getString("attr_name"),
                rs.getString("attr_type")
            )
        }

        return attributes.groupBy { it.typeName }.mapValues { (typeName, attrs) ->
            PostgresTypeInfo(
                typeName = typeName,
                typeCategory = TypeCategory.COMPOSITE,
                attributes = attrs.associate { it.attributeName to it.attributeType }
            )
        }
    }

    private fun loadDomainTypes(): Map<String, PostgresTypeInfo> {
        val params = mapOf("schemas" to DatabaseConfig.dbSchemas.toTypedArray())
        val domains = namedParameterJdbcTemplate.query(SQL_QUERY_DOMAIN_TYPES, params) { rs, _ ->
            DomainTypeInfo(
                rs.getString("domain_name"),
                rs.getString("base_type_name")
            )
        }

        return domains.associate { domainInfo ->
            domainInfo.domainName to PostgresTypeInfo(
                typeName = domainInfo.domainName,
                baseTypeName = domainInfo.baseTypeName,
                typeCategory = TypeCategory.DOMAIN
            )
        }
    }

    companion object {
        private const val SQL_QUERY_ENUM_TYPES = """
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

        private const val SQL_QUERY_COMPOSITE_TYPES = """
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

        private const val SQL_QUERY_DOMAIN_TYPES = """
            SELECT
                t.typname as domain_name,
                bt.typname as base_type_name
            FROM
                pg_type t
                JOIN pg_namespace n ON n.oid = t.typnamespace
                JOIN pg_type bt ON t.typbasetype = bt.oid
            WHERE
                t.typtype = 'd' -- 'd' for domain
                AND n.nspname = ANY(:schemas)
        """
    }
}