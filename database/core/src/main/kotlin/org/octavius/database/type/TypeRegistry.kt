package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.exception.TypeRegistryException
import org.octavius.data.exception.TypeRegistryExceptionMessage
import kotlin.reflect.KClass

internal class TypeRegistry(
    // Główny router: PgTypeName -> Category
    private val categoryMap: Map<String, TypeCategory>,

    // Wyspecjalizowane mapy detali
    private val enums: Map<String, PgEnumDefinition>,
    private val composites: Map<String, PgCompositeDefinition>,
    private val arrays: Map<String, PgArrayDefinition>,

    // Mapowania do zapisu (Kotlin Class -> PgType)
    private val classToPgNameMap: Map<KClass<*>, String>,

    // Mapowania dynamiczne (Dynamic Key -> Kotlin Class)
    private val dynamicDtoMapping: Map<String, KClass<*>>
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    // --- Sekcja ODCZYTU (DB -> Kotlin) ---

    fun getCategory(pgTypeName: String): TypeCategory {
        return categoryMap[pgTypeName] ?: throw TypeRegistryException(
            TypeRegistryExceptionMessage.PG_TYPE_NOT_FOUND,
            typeName = pgTypeName
        )
    }

    fun getEnumDefinition(pgTypeName: String): PgEnumDefinition {
        return enums[pgTypeName] ?: throw TypeRegistryException(
            TypeRegistryExceptionMessage.PG_TYPE_NOT_FOUND,
            typeName = "$pgTypeName (expected ENUM)"
        )
    }

    fun getCompositeDefinition(pgTypeName: String): PgCompositeDefinition {
        return composites[pgTypeName] ?: throw TypeRegistryException(
            TypeRegistryExceptionMessage.PG_TYPE_NOT_FOUND,
            typeName = "$pgTypeName (expected COMPOSITE)"
        )
    }

    fun getArrayDefinition(pgTypeName: String): PgArrayDefinition {
        return arrays[pgTypeName] ?: throw TypeRegistryException(
            TypeRegistryExceptionMessage.PG_TYPE_NOT_FOUND,
            typeName = "$pgTypeName (expected ARRAY)"
        )
    }

    // Dla typu DYNAMIC potrzebujemy znaleźć klasę na podstawie klucza z JSON-a
    fun getDynamicMappableClass(dynamicTypeName: String): KClass<*> {
        return dynamicDtoMapping[dynamicTypeName] ?: throw TypeRegistryException(
            TypeRegistryExceptionMessage.DYNAMIC_TYPE_NOT_FOUND,
            typeName = dynamicTypeName
        )
    }

    // --- Sekcja ZAPISU (Kotlin -> DB) ---

    fun getPgTypeNameForClass(clazz: KClass<*>): String {
        // Bezpośrednie pobranie z mapy po obiekcie klasy
        return classToPgNameMap[clazz] ?: throw TypeRegistryException(
            TypeRegistryExceptionMessage.KOTLIN_CLASS_NOT_MAPPED,
            typeName = clazz.qualifiedName ?: clazz.simpleName ?: "unknown"
        )
    }

    // Metoda pomocnicza dla DynamicDTO
    fun getDynamicTypeNameForClass(clazz: KClass<*>): String? {
        // To jest odwrotność dynamicDtoMapping, można by to scache'ować w init jeśli wolne
        return dynamicDtoMapping.entries.find { it.value == clazz }?.key
    }

    fun isPgType(kClass: KClass<*>): Boolean {
        return classToPgNameMap.containsKey(kClass)
    }
}