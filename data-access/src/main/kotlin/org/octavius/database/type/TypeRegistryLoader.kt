package org.octavius.database.type

import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.octavius.data.contract.EnumCaseConvention
import org.octavius.data.contract.PgType
import org.octavius.database.DatabaseConfig
import org.octavius.exception.TypeRegistryException
import org.octavius.util.Converters
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Buduje instancję [TypeRegistry] poprzez RÓWNOLEGŁE skanowanie bazy danych i klas domenowych.
 *
 * Ta klasa jest odpowiedzialna za łączenie się z bazą danych,
 * wykonywanie zapytań do katalogów systemowych PostgreSQL oraz skanowanie classpath
 * w poszukiwaniu adnotacji. Używa korutyn do maksymalizacji wydajności.
 *
 * @param namedParameterJdbcTemplate Template JDBC do odpytywania bazy danych.
 */
class TypeRegistryLoader(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    // Klasy pomocnicze do przetwarzania wyników z bazy
    private data class DbTypeRawInfo(val infoType: String, val typeName: String, val col1: String?, val col2: String?)
    private data class ProcessedDbTypes(
        val enums: Map<String, PostgresTypeInfo>,
        val composites: Map<String, PostgresTypeInfo>,
        val domains: Map<String, PostgresTypeInfo>
    )

    // Klasa pomocnicza do przechowywania zmapowanych informacji o klasach Kotlina.
    private data class KotlinPgTypeMapping(
        val classFullPath: String,
        val pgTypeName: String,
        val enumConvention: EnumCaseConvention?
    )

    /**
     * Główna metoda, która wykonuje wszystkie kroki i zwraca gotowy,
     * w pełni skonfigurowany [TypeRegistry]. Jest to funkcja zawieszająca (suspend).
     */
    suspend fun load(): TypeRegistry {
        try {
            logger.info { "Starting TypeRegistry loading..." }

            // Używamy coroutineScope, aby zapewnić, że funkcja nie zakończy się,
            // dopóki wszystkie wewnętrzne korutyny (async) nie zakończą pracy.
            val (kotlinTypeMappings, processedDbTypes) = coroutineScope {
                // Krok 1: Uruchom skanowanie classpathu i zapytania do bazy RÓWNOLEGLE
                logger.debug { "Starting parallel execution of classpath scan and database query." }
                val kotlinMappingsJob = async(Dispatchers.IO) { scanDomainClasses() }
                val dbTypesJob = async(Dispatchers.IO) { loadAllCustomTypesFromDb() }

                // Krok 2: Poczekaj na wyniki obu operacji
                val mappings = kotlinMappingsJob.await()
                val rawDbData = dbTypesJob.await()
                logger.debug { "Classpath scan and database query completed." }

                // Krok 3: Przetwórz surowe dane z bazy
                val processed = processRawDbTypes(rawDbData, mappings)

                mappings to processed
            }

            val postgresTypeMap = buildPostgresTypeMap(processedDbTypes)
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
            throw TypeRegistryException("Failed to initialize TypeRegistry", e)
        }
    }

    /**
     * Wykonuje jedno, zunifikowane zapytanie do bazy, aby pobrać wszystkie potrzebne metadane.
     */
    private fun loadAllCustomTypesFromDb(): List<DbTypeRawInfo> {
        logger.debug { "Executing unified query for all custom DB types..." }
        val params = mapOf("schemas" to DatabaseConfig.dbSchemas.toTypedArray())
        return namedParameterJdbcTemplate.query(SQL_QUERY_ALL_TYPES, params) { rs, _ ->
            DbTypeRawInfo(
                infoType = rs.getString("info_type"),
                typeName = rs.getString("type_name"),
                col1 = rs.getString("col1"),
                col2 = rs.getString("col2")
            )
        }
    }

    /**
     * Przetwarza surową listę z bazy na zorganizowane mapy typów.
     */
    private fun processRawDbTypes(
        rawInfo: List<DbTypeRawInfo>,
        kotlinMappings: List<KotlinPgTypeMapping>
    ): ProcessedDbTypes {
        logger.debug { "Processing ${rawInfo.size} raw DB type entries..." }
        val pgNameToEnumConventionMap = kotlinMappings
            .filter { it.enumConvention != null }
            .associate { it.pgTypeName to it.enumConvention!! }

        val enums = mutableMapOf<String, MutableList<String>>()
        val composites = mutableMapOf<String, MutableMap<String, String>>()
        val domains = mutableMapOf<String, String>()

        rawInfo.forEach {
            when (it.infoType) {
                "enum" -> enums.getOrPut(it.typeName) { mutableListOf() }.add(it.col1!!)
                "composite" -> composites.getOrPut(it.typeName) { mutableMapOf() }[it.col1!!] = it.col2!!
                "domain" -> domains[it.typeName] = it.col1!!
            }
        }

        val enumTypes = enums.mapValues { (typeName, values) ->
            PostgresTypeInfo(
                typeName, TypeCategory.ENUM, enumValues = values,
                enumConvention = pgNameToEnumConventionMap[typeName] ?: EnumCaseConvention.SNAKE_CASE_UPPER
            )
        }

        val compositeTypes = composites.mapValues { (typeName, attrs) ->
            PostgresTypeInfo(typeName, TypeCategory.COMPOSITE, attributes = attrs)
        }

        val domainTypes = domains.mapValues { (typeName, baseType) ->
            PostgresTypeInfo(typeName, TypeCategory.DOMAIN, baseTypeName = baseType)
        }

        return ProcessedDbTypes(enumTypes, compositeTypes, domainTypes)
    }

    private fun buildPostgresTypeMap(processedDbTypes: ProcessedDbTypes): Map<String, PostgresTypeInfo> {
        return buildMap {
            putAll(loadStandardTypes())
            putAll(processedDbTypes.enums)
            putAll(processedDbTypes.composites)
            putAll(processedDbTypes.domains)

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
                .acceptPackages("org.octavius")
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
            val ex = TypeRegistryException("FATAL: Failed to load TypeRegistry. Application state is inconsistent!", e)
            logger.error(ex) { "Błąd podczas skanowania klas z adnotacją @PgType" }
            throw ex
        }
        return mappings
    }

    private fun loadStandardTypes(): Map<String, PostgresTypeInfo> {
// Mapowanie standardowych typów PostgreSQL
        return listOf(
            // Typy stałoprzecinkowe
            "serial", "bigserial", "smallserial", "int4", "int8", "int2",
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

    companion object {
        private val logger = KotlinLogging.logger {}

        private const val SQL_QUERY_ENUM_TYPES = """
            SELECT
                'enum' AS info_type,
                t.typname AS type_name,
                e.enumlabel AS col1,
                NULL AS col2,
                e.enumsortorder::int AS sort_order
            FROM
                pg_type t
                JOIN pg_enum e ON t.oid = e.enumtypid
                JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace
            WHERE
                n.nspname = ANY(:schemas)
        """

        private const val SQL_QUERY_COMPOSITE_TYPES = """
            SELECT
                'composite' AS info_type,
                t.typname AS type_name,
                a.attname AS col1,
                at.typname AS col2,
                a.attnum AS sort_order
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
        """

        private const val SQL_QUERY_DOMAIN_TYPES = """
            SELECT
                'domain' AS info_type,
                t.typname as type_name,
                bt.typname as col1,
                NULL AS col2,
                1 AS sort_order -- Kluczowa zmiana: stała wartość, bo nie ma wewnętrznej kolejności
            FROM
                pg_type t
                JOIN pg_namespace n ON n.oid = t.typnamespace
                JOIN pg_type bt ON t.typbasetype = bt.oid
            WHERE
                t.typtype = 'd' -- 'd' for domain
                AND n.nspname = ANY(:schemas)
        """

        // Jedno zapytanie, by rządzić wszystkimi!
        private const val SQL_QUERY_ALL_TYPES = """
            $SQL_QUERY_ENUM_TYPES
            UNION ALL
            $SQL_QUERY_COMPOSITE_TYPES
            UNION ALL
            $SQL_QUERY_DOMAIN_TYPES
            ORDER BY
                type_name, sort_order
        """
    }
}