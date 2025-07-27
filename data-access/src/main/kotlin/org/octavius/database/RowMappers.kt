package org.octavius.database

import org.octavius.data.contract.ColumnInfo
import org.postgresql.jdbc.PgResultSetMetaData
import org.springframework.jdbc.core.RowMapper

/**
 * Fabryka mapperów do konwersji ResultSet na obiekty Kotlin.
 * 
 * Zapewnia różne typy mapperów dla różnych potrzeb aplikacji:
 * - ColumnInfoMapper: do formularzy z pełnymi metadanymi kolumn
 * - ColumnNameMapper: do raportów z mapowaniem przez nazwy kolumn
 * - SingleValueMapper: do zapytań zwracających pojedynczą wartość
 * 
 * Wszystkie mappery używają DatabaseToKotlinTypesConverter do konwersji typów.
 */

/**
 * Główna klasa dostarczająca mapper do konwersji wyników zapytań.
 * 
 * @param typesConverter Konwerter typów PostgreSQL na typy Kotlin
 * 
 * Przykład użycia:
 * ```kotlin
 * val mappers = RowMappers(typesConverter)
 * val results = jdbcTemplate.query(sql, params, mappers.ColumnNameMapper())
 * ```
 */
class RowMappers(private val typesConverter: DatabaseToKotlinTypesConverter) {

    /**
     * Mapper do konwersji ResultSet na mapę ColumnInfo.
     * 
     * Używany głównie w formularzach gdzie potrzebne są pełne metadane kolumn
     * (nazwa tabeli + nazwa kolumny). Pozwala na identyfikację skąd pochodzi dana kolumna
     * w zapytaniach z JOIN.
     * 
     * @return RowMapper mapujący na Map<ColumnInfo, Any?>
     */
    fun ColumnInfoMapper() : RowMapper<Map<ColumnInfo, Any?>> = RowMapper { rs, _ ->
        val data = mutableMapOf<ColumnInfo, Any?>()
        val metaData = rs.metaData as PgResultSetMetaData

        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val tableName = metaData.getTableName(i)
            val columnType = metaData.getColumnTypeName(i)

            val rawValue = rs.getString(i)

            val convertedValue = typesConverter.convertToDomainType(rawValue, columnType)
            data[ColumnInfo(tableName, columnName)] = convertedValue
        }

        data
    }

    /**
     * Mapper do konwersji ResultSet na mapę String (nazwa kolumny) -> wartość.
     * 
     * Używany głównie w raportach gdzie ważna jest tylko nazwa kolumny,
     * a nie jej pochodzenie z konkretnej tabeli.
     * 
     * @return RowMapper mapujący na Map<String, Any?>
     */
    fun ColumnNameMapper(): RowMapper<Map<String, Any?>> = RowMapper { rs, _ ->
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData as PgResultSetMetaData
        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnTypeName(i)
            val rawValue = rs.getString(i)
            // Konwersja przez typesConverter z uwzględnieniem typu PostgreSQL
            data[columnName] = typesConverter.convertToDomainType(rawValue, columnType)
        }
        data
    }
    
    /**
     * Mapper do konwersji pojedynczej wartości z ResultSet.
     * 
     * Używany dla zapytań zwracających tylko jedną kolumnę,
     * np. COUNT(*), MAX(id), SELECT name WHERE id = 1.
     * 
     * @return RowMapper mapujący na Any (typ zależy od typu kolumny PostgreSQL)
     */
    fun SingleValueMapper(): RowMapper<Any> = RowMapper { rs, _ ->
        val columnType = (rs.metaData as PgResultSetMetaData).getColumnTypeName(1)
        val rawValue = rs.getString(1)
        typesConverter.convertToDomainType(rawValue, columnType)
    }
}