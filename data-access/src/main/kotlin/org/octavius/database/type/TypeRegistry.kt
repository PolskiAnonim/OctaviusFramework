package org.octavius.database.type

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

    /**
     * Zwraca informacje o typie PostgreSQL.
     *
     * @param pgTypeName Nazwa typu w PostgreSQL.
     * @return Informacje o typie lub null, jeśli typ nie został znaleziony.
     */
    fun getTypeInfo(pgTypeName: String): PostgresTypeInfo? {
        return postgresTypeMap[pgTypeName]
    }

    /**
     * Zwraca nazwę typu PostgreSQL powiązaną z daną klasą Kotlina.
     *
     * @param clazz Klasa Kotlina (KClass).
     * @return Nazwa typu w PostgreSQL lub null, jeśli nie znaleziono mapowania.
     */
    fun getPgTypeNameForClass(clazz: KClass<*>): String? {
        return classFullPathToPgTypeNameMap[clazz.qualifiedName]
    }

    /**
     * Zwraca pełną ścieżkę klasy Kotlina powiązaną z daną nazwą typu PostgreSQL.
     *
     * @param pgTypeName Nazwa typu w PostgreSQL.
     * @return Pełna ścieżka klasy (np. "org.example.MyEnum") lub null.
     */
    fun getClassFullPathForPgTypeName(pgTypeName: String): String? {
        return pgTypeNameToClassFullPathMap[pgTypeName]
    }

    /**
     * Zwraca wszystkie zarejestrowane typy PostgreSQL.
     *
     * @return Niemodyfikowalna mapa nazw typów na informacje o typach.
     */
    fun getAllRegisteredTypes(): Map<String, PostgresTypeInfo> {
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