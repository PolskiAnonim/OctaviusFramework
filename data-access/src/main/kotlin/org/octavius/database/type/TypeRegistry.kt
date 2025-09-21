package org.octavius.database.type

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.EnumCaseConvention
import org.octavius.exception.TypeRegistryException
import kotlin.reflect.KClass

/**
 * Rejestr typów - mapowanie między typami PostgreSQL a klasami Kotlina.
 *
 * Niemutowalny rejestr przechowujący metadane o typach z bazy danych oraz mapowania
 * na klasy domenowe oznaczone adnotacją @PgType. Inicjalizowany przez [TypeRegistryLoader]
 * na podstawie skanowania bazy danych i classpath.
 *
 * Obsługiwane kategorie typów:
 * - STANDARD - podstawowe typy PostgreSQL (int4, text, bool, itp.)
 * - ENUM - typy wyliczeniowe z automatyczną konwersją konwencji nazw
 * - ARRAY - typy tablicowe z rekurencyjnym przetwarzaniem elementów
 * - COMPOSITE - typy kompozytowe mapowane na data class
 * - DOMAIN - typy domenowe delegujące do typów bazowych
 *
 * @property postgresTypeMap Mapa nazw typów PostgreSQL na szczegółowe informacje o typach
 * @property classFullPathToPgTypeNameMap Mapa pełnych ścieżek klas Kotlina na nazwy typów PostgreSQL
 * @property pgTypeNameToClassFullPathMap Mapa nazw typów PostgreSQL na pełne ścieżki klas Kotlina
 */
internal class TypeRegistry(
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
     * @return Informacje o typie.
     * @throws TypeRegistryException jeśli typ nie został znaleziony w rejestrze.
     */
    fun getTypeInfo(pgTypeName: String): PostgresTypeInfo {
        val typeInfo = postgresTypeMap[pgTypeName]
        logger.trace { "Looking up type info for '$pgTypeName': ${if (typeInfo != null) "found" else "not found"}" }
        return typeInfo ?: throw TypeRegistryException("Nie znaleziono danych typu PostgreSQL.")
    }

    /**
     * Zwraca nazwę typu PostgreSQL powiązaną z daną klasą Kotlina.
     *
     * @param clazz Klasa Kotlina (KClass) oznaczona adnotacją @PgType.
     * @return Nazwa typu w PostgreSQL.
     * @throws TypeRegistryException jeśli klasa nie jest zarejestrowanym typem PostgreSQL.
     */
    fun getPgTypeNameForClass(clazz: KClass<*>): String {
        val pgTypeName = classFullPathToPgTypeNameMap[clazz.qualifiedName]
        logger.trace { "Looking up PostgreSQL type for class '${clazz.qualifiedName}': ${pgTypeName ?: "not found"}" }
        return pgTypeName ?: throw TypeRegistryException("Klasa ${clazz.qualifiedName} nie jest zarejestrowanym typem PostgreSQL. Czy ma adnotację @PgType?")
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
internal enum class TypeCategory {
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
internal data class PostgresTypeInfo(
    val typeName: String,
    val typeCategory: TypeCategory,
    val enumValues: List<String> = emptyList(),
    val enumConvention: EnumCaseConvention = EnumCaseConvention.SNAKE_CASE_LOWER,
    val elementType: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val baseTypeName: String? = null
)