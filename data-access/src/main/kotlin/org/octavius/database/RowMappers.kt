package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.toDataObject
import org.octavius.database.type.PostgresToKotlinConverter
import org.octavius.exception.DataMappingException
import org.postgresql.jdbc.PgResultSetMetaData
import org.springframework.jdbc.core.RowMapper
import kotlin.reflect.KClass

/**
 * Fabryka dostarczająca różne implementacje `RowMapper` do konwersji `ResultSet`.
 *
 * @param typesConverter Konwerter typów PostgreSQL na typy Kotlina, używany przez wszystkie mappery.
 */
internal class RowMappers(private val typesConverter: PostgresToKotlinConverter) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Mapper mapujący na `Map<String, Any?>`.
     * Używa tylko nazwy kolumny jako klucza. Idealny do raportów i prostych zapytań.
     */
    fun ColumnNameMapper(): RowMapper<Map<String, Any?>> = RowMapper { rs, _ ->
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData as PgResultSetMetaData
        
        logger.trace { "Mapping row with ${metaData.columnCount} columns using ColumnNameMapper" }
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
    fun SingleValueMapper(): RowMapper<Any?> = RowMapper { rs, _ ->
        val columnType = (rs.metaData as PgResultSetMetaData).getColumnTypeName(1)
        val rawValue = rs.getString(1)
        typesConverter.convertToDomainType(rawValue, columnType)
    }

    /**
     * Generyczny mapper, który konwertuje wiersz na obiekt data class.
     * Najpierw mapuje wiersz na Map<String, Any?> za pomocą ColumnNameMapper,
     * a następnie używa refleksji (przez `toDataObject`), aby utworzyć instancję klasy.
     * @param kClass Klasa docelowego obiektu.
     */
    fun <T : Any> DataObjectMapper(kClass: KClass<T>): RowMapper<T> {
        val baseMapper = ColumnNameMapper()
        return RowMapper { rs, rowNum ->
            logger.trace { "Mapping row to ${kClass.simpleName} using DataObjectMapper" }
            val map = baseMapper.mapRow(rs, rowNum)

            try {
                // Używamy istniejącej logiki do konwersji mapy na obiekt
                val result = map.toDataObject(kClass)
                logger.trace { "Successfully mapped row to ${kClass.simpleName}" }
                result
            } catch (e: Exception) {
                // Najbogatszy kontekst: co, na co i z czego próbowaliśmy mapować.
                val mappingEx = DataMappingException(
                    message = "Failed to map row to ${kClass.simpleName}",
                    targetClass = kClass.qualifiedName ?: kClass.simpleName ?: "unknown class",
                    rowData = map,
                    cause = e
                )
                logger.error(mappingEx) {
                    "Failed to map row to ${mappingEx.targetClass}. Problematic row data: ${mappingEx.rowData}"
                }
                throw mappingEx
            }
        }
    }
}