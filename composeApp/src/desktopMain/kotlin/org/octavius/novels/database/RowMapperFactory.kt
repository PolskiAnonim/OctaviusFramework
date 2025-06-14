package org.octavius.novels.database

import org.octavius.novels.domain.ColumnInfo
import org.postgresql.jdbc.PgResultSetMetaData
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class RowMappers(private val typesConverter: UserTypesConverter) {
    fun ColumnNameMapper(): RowMapper<Map<String, Any?>> = RowMapper { rs, _ ->
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData as PgResultSetMetaData
        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnTypeName(i)
            val rawValue = rs.getObject(i)
            // rawValue jest już określonego typu bądź jest nullem
            data[columnName] = typesConverter.convertToDomainType(rawValue, columnType)
        }
        data
    }
    
    fun SingleValueMapper(): RowMapper<Any> = RowMapper { rs, _ ->
        val columnType = (rs.metaData as PgResultSetMetaData).getColumnTypeName(1)
        val rawValue = rs.getObject(1)
        typesConverter.convertToDomainType(rawValue, columnType)
    }
}


class RowMapperFactory(private val typesConverter: UserTypesConverter) {

    // Główny mapper używany wszędzie - zwraca Map<ColumnInfo, Any?>
    fun createColumnInfoMapper(): RowMapper<Map<ColumnInfo, Any?>> = RowMapper { rs, _ ->
        mapRowToColumnInfo(rs)
    }

    // ResultSetExtractor dla przypadków gdy potrzebujemy tylko jednego wiersza
    fun createSingleRowExtractor(): ResultSetExtractor<Map<ColumnInfo, Any?>> = ResultSetExtractor { rs ->
        if (rs.next()) {
            mapRowToColumnInfo(rs)
        } else {
            emptyMap()
        }
    }

    // Wspólna logika mapowania
    private fun mapRowToColumnInfo(rs: ResultSet): Map<ColumnInfo, Any?> {
        val data = mutableMapOf<ColumnInfo, Any?>()
        val metaData = rs.metaData as PgResultSetMetaData

        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val tableName = metaData.getTableName(i)
            val columnType = metaData.getColumnTypeName(i)

            val rawValue = rs.getObject(i)
            if (rs.wasNull()) {
                data[ColumnInfo(tableName, columnName)] = null
            } else {
                val convertedValue = typesConverter.convertToDomainType(rawValue, columnType)
                data[ColumnInfo(tableName, columnName)] = convertedValue
            }
        }

        return data
    }
}