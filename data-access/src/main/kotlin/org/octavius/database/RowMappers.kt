package org.octavius.database

import org.octavius.data.contract.ColumnInfo
import org.postgresql.jdbc.PgResultSetMetaData
import org.springframework.jdbc.core.RowMapper

/**
 * Fabryka dostarczająca różne implementacje `RowMapper` do konwersji `ResultSet`.
 *
 * @param typesConverter Konwerter typów PostgreSQL na typy Kotlina, używany przez wszystkie mappery.
 */
class RowMappers(private val typesConverter: PostgresToKotlinConverter) {

    /**
     * Mapper mapujący na `Map<ColumnInfo, Any?>`.
     * Przechowuje nazwę tabeli i kolumny. Używany głównie w formularzach,
     * aby rozróżnić kolumny o tej samej nazwie z różnych tabel połączonych JOIN-em.
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
     * Mapper mapujący na `Map<String, Any?>`.
     * Używa tylko nazwy kolumny jako klucza. Idealny do raportów i prostych zapytań.
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
     * Mapper mapujący wynik z pojedynczej kolumny na jego wartość.
     * Używany dla zapytań typu `SELECT COUNT(*)`, `SELECT id FROM ...` itp.
     */
    fun SingleValueMapper(): RowMapper<Any> = RowMapper { rs, _ ->
        val columnType = (rs.metaData as PgResultSetMetaData).getColumnTypeName(1)
        val rawValue = rs.getString(1)
        typesConverter.convertToDomainType(rawValue, columnType)
    }
}