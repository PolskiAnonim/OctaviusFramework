package org.octavius.database.type

import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import org.postgresql.jdbc.PgResultSetMetaData
import java.sql.ResultSet

/**
 * Inteligentnie wyodrębnia wartości z ResultSet.
 * Używa "szybkiej ścieżki" (metody natywne rs.get*()) dla typów standardowych
 * i deleguje do PostgresToKotlinConverter dla typów złożonych (enum, composite, array).
 */
internal class ResultSetValueExtractor(
    private val typeRegistry: TypeRegistry
) {
    private val stringConverter = PostgresToKotlinConverter(typeRegistry)
    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun extract(rs: ResultSet, columnIndex: Int): Any? {
        // Sprawdzenie, czy wartość jest SQL NULL
        if (rs.getObject(columnIndex) == null) {
            return null
        }

        val pgTypeName = (rs.metaData as PgResultSetMetaData).getColumnTypeName(columnIndex)
        val typeInfo = typeRegistry.getTypeInfo(pgTypeName)

        // Główna logika: rozróżnienie ścieżek
        return when (typeInfo.typeCategory) {
            TypeCategory.STANDARD -> extractStandardType(rs, columnIndex, pgTypeName)
            else -> {
                val rawValue = rs.getString(columnIndex)
                stringConverter.convert(rawValue, pgTypeName)
            }
        }
    }

    /**
     * Szybka ścieżka dla typów standardowych.
     * Używa natywnych metod get* z ResultSet, omijając konwersję do String i z powrotem.
     */
    private fun extractStandardType(rs: ResultSet, columnIndex: Int, pgTypeName: String): Any? {
        // Używamy rs.getObject() i sprawdzamy typ, co jest bezpieczniejsze
        // niż ślepe wywoływanie rs.getInt() itp.
        // Lub, dla maksymalnej wydajności, możemy mieć tu when po pgTypeName.
        return when (pgTypeName) {
            "int4", "serial", "int2", "smallserial" -> rs.getInt(columnIndex)
            "int8", "bigserial" -> rs.getLong(columnIndex)
            "float4" -> rs.getFloat(columnIndex)
            "float8" -> rs.getDouble(columnIndex)
            "numeric" -> rs.getBigDecimal(columnIndex)
            "bool" -> rs.getBoolean(columnIndex)
            "uuid" -> rs.getObject(columnIndex) as java.util.UUID
            "date" -> rs.getDate(columnIndex).toLocalDate().toKotlinLocalDate()
            "timestamp" -> rs.getTimestamp(columnIndex).toLocalDateTime().toKotlinLocalDateTime()
            "timestamptz", "time", "timetz", "interval", "json", "jsonb" -> {
                val rawValue = rs.getString(columnIndex)
                stringConverter.convert(rawValue, pgTypeName)
            }
            // text, varchar, char etc.
            else -> rs.getString(columnIndex)
        }
    }
}