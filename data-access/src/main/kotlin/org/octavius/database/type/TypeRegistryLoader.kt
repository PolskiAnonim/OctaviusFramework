package org.octavius.database.type

import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.octavius.data.PgStandardType
import org.octavius.data.annotation.DynamicallyMappable
import org.octavius.data.annotation.EnumCaseConvention
import org.octavius.data.annotation.PgType
import org.octavius.data.exception.TypeRegistryException
import org.octavius.data.exception.TypeRegistryExceptionMessage
import org.octavius.data.util.toSnakeCase
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.reflect.KClass

internal class TypeRegistryLoader(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val packagesToScan: List<String>,
    private val dbSchemas: List<String>
) {

    private data class DbTypeRawInfo(val infoType: String, val typeName: String, val col1: String?, val col2: String?)
    private data class ProcessedDbTypes(
        val enums: Map<String, PostgresTypeInfo>,
        val composites: Map<String, PostgresTypeInfo>
    )

    // Klasa pomocnicza do przechowywania zmapowanych informacji o klasach Kotlina.
    private data class KotlinPgTypeMapping(
        val classFullPath: String,
        val pgTypeName: String,
        val enumConvention: EnumCaseConvention?
    )

    private data class KotlinDynamicTypeMapping(val typeName: String, val kClass: KClass<*>)

    // klasa-kontener na wyniki z jednego skanowania
    private data class AnnotationScanResults(
        val pgTypeMappings: List<KotlinPgTypeMapping>,
        val dynamicTypeMappings: List<KotlinDynamicTypeMapping>
    )

    suspend fun load(): TypeRegistry {
        try {
            logger.info { "Starting TypeRegistry loading..." }

            // Zmiana: teraz mamy tylko DWA równoległe zadania
            val (scanResults, processedDbTypes) = coroutineScope {
                logger.debug { "Starting parallel execution of classpath scan and database query." }
                // JEDNO skanowanie classpathu
                val annotationScanJob = async(Dispatchers.IO) { scanForAnnotations() }
                val dbTypesJob = async(Dispatchers.IO) { loadAllCustomTypesFromDb() }

                val scanRes = annotationScanJob.await()
                val rawDbData = dbTypesJob.await()
                logger.debug { "Classpath scan and database query completed." }

                val processed = processRawDbTypes(rawDbData, scanRes.pgTypeMappings)

                scanRes to processed
            }

            val postgresTypeMap = buildPostgresTypeMap(processedDbTypes)
            // Rozpakowujemy wyniki ze zunifikowanego skanowania
            val (classToPgMap, pgToClassMap) = buildBidirectionalClassMaps(scanResults.pgTypeMappings)
            val dynamicTypeMap = scanResults.dynamicTypeMappings.associate { it.typeName to it.kClass }

            logger.info { "TypeRegistry loaded successfully. Found ${postgresTypeMap.size} total PG types." }
            logger.debug { "Static @PgStandardType.kt mappings found: ${scanResults.pgTypeMappings.size}" }
            logger.debug { "Dynamic @DynamicallyMappable mappings found: ${scanResults.dynamicTypeMappings.size}" }

            return TypeRegistry(
                postgresTypeMap = postgresTypeMap,
                classFullPathToPgTypeNameMap = classToPgMap,
                pgTypeNameToClassFullPathMap = pgToClassMap,
                dynamicTypeNameToKClassMap = dynamicTypeMap
            )
        } catch (e: TypeRegistryException) {
            // Jeśli już jest to nasz typ, po prostu go rzuć dalej
            throw e
        } catch (e: Exception) {
            logger.error(e) { "FATAL: Failed to load TypeRegistry. Application state is inconsistent!" }
            // Opakuj ogólny błąd
            throw TypeRegistryException(TypeRegistryExceptionMessage.INITIALIZATION_FAILED, cause = e)
        }
    }

    /**
     * Wykonuje JEDNO skanowanie classpathu i wyszukuje klasy z obiema adnotacjami.
     */
    private fun scanForAnnotations(): AnnotationScanResults {
        val pgMappings = mutableListOf<KotlinPgTypeMapping>()
        val dynamicMappings = mutableListOf<KotlinDynamicTypeMapping>()

        try {
            logger.debug { "Scanning packages for annotations: ${packagesToScan.joinToString()}" }
            ClassGraph()
                .enableAllInfo()                    // DynamicDto
                .acceptPackages("org.octavius.database.type", *packagesToScan.toTypedArray())
                .scan().use { scanResult ->
                    // Przetwarzamy klasy z @PgType
                    scanResult.getClassesWithAnnotation(PgType::class.java).forEach { classInfo ->
                        val annotationInfo = classInfo.getAnnotationInfo(PgType::class.java)
                        val pgTypeNameFromAnnotation = annotationInfo.parameterValues.getValue("name") as String
                        val pgTypeName =
                            pgTypeNameFromAnnotation.ifBlank { classInfo.simpleName.toSnakeCase() }

                        var convention: EnumCaseConvention? = null
                        if (classInfo.isEnum) {
                            val conventionEnumValue =
                                annotationInfo.parameterValues.getValue("enumConvention") as? io.github.classgraph.AnnotationEnumValue
                            convention = conventionEnumValue?.loadClassAndReturnEnumValue() as? EnumCaseConvention
                        }
                        pgMappings.add(KotlinPgTypeMapping(classInfo.name, pgTypeName, convention))
                    }

                    // Przetwarzamy klasy z @DynamicallyMappable
                    scanResult.getClassesWithAnnotation(DynamicallyMappable::class.java).forEach { classInfo ->
                        val annotationInfo = classInfo.getAnnotationInfo(DynamicallyMappable::class.java)
                        val typeName = annotationInfo.parameterValues.getValue("typeName") as String
                        dynamicMappings.add(KotlinDynamicTypeMapping(typeName, classInfo.loadClass().kotlin))
                    }
                }
        } catch (e: Exception) {
            val ex = TypeRegistryException(TypeRegistryExceptionMessage.CLASSPATH_SCAN_FAILED, cause = e)
            logger.error(ex) { ex.message }
            throw ex
        }
        return AnnotationScanResults(pgMappings, dynamicMappings)
    }


    private fun loadAllCustomTypesFromDb(): List<DbTypeRawInfo> {
        try {
            logger.debug { "Executing unified query for all custom DB types..." }
            // Używamy schematów przekazanych w konstruktorze
            val params = mapOf("schemas" to dbSchemas.toTypedArray())
            return namedParameterJdbcTemplate.query(SQL_QUERY_ALL_TYPES, params) { rs, _ ->
                DbTypeRawInfo(
                    infoType = rs.getString("info_type"),
                    typeName = rs.getString("type_name"),
                    col1 = rs.getString("col1"),
                    col2 = rs.getString("col2")
                )
            }
        } catch (e: Exception) {
            throw TypeRegistryException(TypeRegistryExceptionMessage.DB_QUERY_FAILED, cause = e)
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

        rawInfo.forEach {
            when (it.infoType) {
                "enum" -> enums.getOrPut(it.typeName) { mutableListOf() }.add(it.col1!!)
                "composite" -> composites.getOrPut(it.typeName) { mutableMapOf() }[it.col1!!] = it.col2!!
            }
        }

        val enumTypes = enums.mapValues { (typeName, values) ->
            PostgresTypeInfo(
                typeName, TypeCategory.ENUM, enumValues = values,
                enumConvention = pgNameToEnumConventionMap[typeName] ?: EnumCaseConvention.SNAKE_CASE_UPPER
            )
        }

        val compositeTypes = composites.mapValues { (typeName, attrs) ->
            val category = if (typeName == "dynamic_dto") TypeCategory.DYNAMIC else TypeCategory.COMPOSITE
            PostgresTypeInfo(typeName, category, attributes = attrs)
        }

        return ProcessedDbTypes(enumTypes, compositeTypes)
    }

    private fun buildPostgresTypeMap(processedDbTypes: ProcessedDbTypes): Map<String, PostgresTypeInfo> {
        return buildMap {
            putAll(loadStandardTypes())
            putAll(processedDbTypes.enums)
            putAll(processedDbTypes.composites)

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

    private fun loadStandardTypes(): Map<String, PostgresTypeInfo> {
        // Mapowanie standardowych typów PostgreSQL
        return PgStandardType.entries.filter { !it.isArray }
            .associate { it.typeName to PostgresTypeInfo(it.typeName, TypeCategory.STANDARD) }
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

        // Jedno zapytanie, by rządzić wszystkimi!
        private const val SQL_QUERY_ALL_TYPES = """
            $SQL_QUERY_ENUM_TYPES
            UNION ALL
            $SQL_QUERY_COMPOSITE_TYPES
            ORDER BY
                type_name, sort_order
        """
    }
}