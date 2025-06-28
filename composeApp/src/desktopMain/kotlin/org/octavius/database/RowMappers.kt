package org.octavius.database

import org.octavius.form.ColumnInfo
import org.postgresql.jdbc.PgResultSetMetaData
import org.springframework.jdbc.core.RowMapper

class RowMappers(private val typesConverter: DatabaseToKotlinTypesConverter) {

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

    fun ColumnNameMapper(): RowMapper<Map<String, Any?>> = RowMapper { rs, _ ->
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData as PgResultSetMetaData
        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnTypeName(i)
            val rawValue = rs.getString(i)
            // rawValue jest już określonego typu bądź jest nullem
            data[columnName] = typesConverter.convertToDomainType(rawValue, columnType)
        }
        data
    }
    
    fun SingleValueMapper(): RowMapper<Any> = RowMapper { rs, _ ->
        val columnType = (rs.metaData as PgResultSetMetaData).getColumnTypeName(1)
        val rawValue = rs.getString(1)
        typesConverter.convertToDomainType(rawValue, columnType)
    }
}