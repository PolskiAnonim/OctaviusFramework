package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.EnumCaseConvention
import kotlin.reflect.KClass

/**
 * Przechowuje i udostępnia mapowanie między typami PostgreSQL a klasami Kotlina.
 *
 * Ta klasa jest **niemutowalnym** rejestrem. Jej instancja powinna być tworzona
 * za pomocą [TypeRegistryLoader], który jest odpowiedzialny za zebranie wszystkich
 * niezbędnych danych z bazy i classpath.
 *
 * @property postgresTypeMap Mapa nazw typów PostgreSQL na szczegółowe informacje o typach.
 * @property classFullPathToPgTypeNameMap Mapa pełnych ścieżek klas Kotlina na nazwy typów w PostgreSQL.
 * @property pgTypeNameToClassFullPathMap Mapa nazw typów w PostgreSQL na pełne ścieżki klas Kotlina.
 */
class TypeRegistry internal constructor(
    private val postgresTypeMap: Map<String, PostgresTypeInfo>,
    private val classFullPathToPgTypeNameMap: Map<String, String>,
    private val pgTypeNameToClassFullPathMap: Map<String, String>
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }


    /**
     * Zwraca informacje o typie PostgreSQL.
     *
     * @param pgTypeName Nazwa typu w PostgreSQL.
     * @return Informacje o typie lub null, jeśli typ nie został znaleziony.
     */
    fun getTypeInfo(pgTypeName: String): PostgresTypeInfo? {
        val typeInfo = postgresTypeMap[pgTypeName]
        logger.trace { "Looking up type info for '$pgTypeName': ${if (typeInfo != null) "found" else "not found"}" }
        return typeInfo
    }

    /**
     * Zwraca nazwę typu PostgreSQL powiązaną z daną klasą Kotlina.
     *
     * @param clazz Klasa Kotlina (KClass).
     * @return Nazwa typu w PostgreSQL lub null, jeśli nie znaleziono mapowania.
     */
    fun getPgTypeNameForClass(clazz: KClass<*>): String? {
        val pgTypeName = classFullPathToPgTypeNameMap[clazz.qualifiedName]
        logger.trace { "Looking up PostgreSQL type for class '${clazz.qualifiedName}': ${pgTypeName ?: "not found"}" }
        return pgTypeName
    }

    /**
     * Zwraca pełną ścieżkę klasy Kotlina powiązaną z daną nazwą typu PostgreSQL.
     *
     * @param pgTypeName Nazwa typu w PostgreSQL.
     * @return Pełna ścieżka klasy (np. "org.example.MyEnum") lub null.
     */
    fun getClassFullPathForPgTypeName(pgTypeName: String): String? {
        val classPath = pgTypeNameToClassFullPathMap[pgTypeName]
        logger.trace { "Looking up class for PostgreSQL type '$pgTypeName': ${classPath ?: "not found"}" }
        return classPath
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
enum class TypeCategory {
    /** Typ wyliczeniowy (CREATE TYPE ... AS ENUM) */
    ENUM,
    /** Typ tablicowy (prefikś "_" w nazwie typu) */
    ARRAY,
    /** Typ kompozytowy (CREATE TYPE ... AS) */
    COMPOSITE,
    /** Standardowy typ PostgreSQL (int4, text, bool, itp.) */
    STANDARD,
    /** Typ domenowy, obsługiwany podobnie jak standardowy typ ale jest potrzebna informacja o nim */
    DOMAIN
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
 * @param baseTypeName podstawowy typ dla typu DOMAIN (null dla innych typów)
 */
data class PostgresTypeInfo(
    val typeName: String,
    val typeCategory: TypeCategory,
    val enumValues: List<String> = emptyList(),
    val enumConvention: EnumCaseConvention = EnumCaseConvention.SNAKE_CASE_LOWER,
    val elementType: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val baseTypeName: String? = null
)