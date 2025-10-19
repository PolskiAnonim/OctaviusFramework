package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.annotation.EnumCaseConvention
import org.octavius.data.exception.TypeRegistryException
import kotlin.reflect.KClass

/**
 * Rejestr typów - mapowanie między typami PostgreSQL a klasami Kotlina.
 *
 * Niemutowalny rejestr przechowujący metadane o typach z bazy danych oraz mapowania
 * na klasy domenowe oznaczone adnotacją @PgStandardType.kt. Inicjalizowany przez [TypeRegistryLoader]
 * na podstawie skanowania bazy danych i classpath.
 *
 * Obsługiwane kategorie typów:
 * - STANDARD - podstawowe typy PostgreSQL (int4, text, bool, itp.)
 * - ENUM - typy wyliczeniowe z automatyczną konwersją konwencji nazw
 * - ARRAY - typy tablicowe z rekurencyjnym przetwarzaniem elementów
 * - COMPOSITE - typy kompozytowe mapowane na data class
 *
 * @property postgresTypeMap Mapa nazw typów PostgreSQL na szczegółowe informacje o typach
 * @property classFullPathToPgTypeNameMap Mapa pełnych ścieżek klas Kotlina na nazwy typów PostgreSQL
 * @property pgTypeNameToClassFullPathMap Mapa nazw typów PostgreSQL na pełne ścieżki klas Kotlina
 */
internal class TypeRegistry(
    private val postgresTypeMap: Map<String, PostgresTypeInfo>,
    private val classFullPathToPgTypeNameMap: Map<String, String>,
    private val pgTypeNameToClassFullPathMap: Map<String, String>,
    private val dynamicTypeNameToKClassMap: Map<String, KClass<*>>
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Zwraca klasę Kotlina (KClass) powiązaną z danym kluczem dynamicznego typu.
     *
     * @param dynamicTypeName Klucz (nazwa) z adnotacji @DynamicallyMappable.
     * @return KClass<?> lub null, jeśli nie znaleziono mapowania.
     */
    fun getDynamicMappableClass(dynamicTypeName: String): KClass<*> {
        val kClass = dynamicTypeNameToKClassMap[dynamicTypeName]
        logger.trace { "Looking up dynamic mappable class for key '$dynamicTypeName': ${kClass?.simpleName ?: "not found"}" }
        return kClass ?: throw TypeRegistryException("Nie znaleziono zarejestrowanej klasy dla dynamicznego typu '$dynamicTypeName'")
    }


    /**
     * Zwraca informacje o typie PostgreSQL.
     *
     * @param pgTypeName Nazwa typu w PostgreSQL.
     * @return Informacje o typie.
     * @throws org.octavius.data.exception.TypeRegistryException jeśli typ nie został znaleziony w rejestrze.
     */
    fun getTypeInfo(pgTypeName: String): PostgresTypeInfo {
        val typeInfo = postgresTypeMap[pgTypeName]
        logger.trace { "Looking up type info for '$pgTypeName': ${if (typeInfo != null) "found" else "not found"}" }
        return typeInfo ?: throw TypeRegistryException("Nie znaleziono danych typu PostgreSQL.")
    }

    /**
     * Zwraca nazwę typu PostgreSQL powiązaną z daną klasą Kotlina.
     *
     * @param clazz Klasa Kotlina (KClass) oznaczona adnotacją @PgStandardType.kt.
     * @return Nazwa typu w PostgreSQL.
     * @throws org.octavius.data.exception.TypeRegistryException jeśli klasa nie jest zarejestrowanym typem PostgreSQL.
     */
    fun getPgTypeNameForClass(clazz: KClass<*>): String {
        val pgTypeName = classFullPathToPgTypeNameMap[clazz.qualifiedName]
        logger.trace { "Looking up PostgreSQL type for class '${clazz.qualifiedName}': ${pgTypeName ?: "not found"}" }
        return pgTypeName ?: throw TypeRegistryException("Klasa ${clazz.qualifiedName} nie jest zarejestrowanym typem PostgreSQL. Czy ma adnotację @PgStandardType.kt?")
    }

    /**
     * Zwraca pełną ścieżkę klasy Kotlina powiązaną z daną nazwą typu PostgreSQL.
     *
     * @param pgTypeName Nazwa typu w PostgreSQL.
     * @return Pełna ścieżka klasy (np. "org.example.MyEnum") lub null.
     */
    fun getClassFullPathForPgTypeName(pgTypeName: String): String {
        val classPath = pgTypeNameToClassFullPathMap[pgTypeName]
        logger.trace { "Looking up class for PostgreSQL type '$pgTypeName': ${classPath ?: "not found"}" }
        return classPath ?: throw TypeRegistryException("Nie znaleziono klasy dla typu PostgreSQL '${pgTypeName}'.")
    }

    /**
     * Zwraca wszystkie zarejestrowane typy PostgreSQL.
     *
     * @return Niemodyfikowalna mapa nazw typów na informacje o typach.
     */
    fun getAllRegisteredTypes(): Map<String, PostgresTypeInfo> {
        logger.debug { "Returning ${postgresTypeMap.size} registered PostgreSQL types" }
        return postgresTypeMap
    }
}

/**
 * Kategorie typów PostgreSQL obsługiwane przez system.
 */
internal enum class TypeCategory {
    /** Typ wyliczeniowy (CREATE TYPE ... AS ENUM) */
    ENUM,
    /** Typ tablicowy (prefiks "_" w nazwie typu) */
    ARRAY,
    /** Typ kompozytowy (CREATE TYPE ... AS) */
    COMPOSITE,
    /** Standardowy typ PostgreSQL (int4, text, bool, itp.) */
    STANDARD,
    /* Typ tworzony w locie w zapytaniach przez dynamic_dto */
    DYNAMIC
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
internal data class PostgresTypeInfo(
    val typeName: String,
    val typeCategory: TypeCategory,
    val enumValues: List<String> = emptyList(),
    val enumConvention: EnumCaseConvention = EnumCaseConvention.SNAKE_CASE_LOWER,
    val elementType: String? = null,
    val attributes: Map<String, String> = emptyMap()
)