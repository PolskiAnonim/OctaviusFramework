package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.exception.ConversionException
import org.octavius.data.exception.ConversionExceptionMessage
import org.octavius.data.toDataObject
import org.octavius.database.type.ResultSetValueExtractor
import org.springframework.jdbc.core.RowMapper
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

/**
 * Fabryka dostarczająca różne implementacje `RowMapper` do konwersji `ResultSet`.
 *
 * @param valueExtractor Odzyskiwacz/wydobywacz wartości z bazy
 */
@Suppress("FunctionName")
internal class RowMappers(
    private val valueExtractor: ResultSetValueExtractor
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Mapper mapujący na `Map<String, Any?>`.
     * Używa tylko nazwy kolumny jako klucza. Idealny do raportów i prostych zapytań.
     */
    fun ColumnNameMapper(): RowMapper<Map<String, Any?>> = RowMapper { rs, _ ->
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData

        logger.trace { "Mapping row with ${metaData.columnCount} columns using ColumnNameMapper" }
        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnLabel(i)
            data[columnName] = valueExtractor.extract(rs, i)
        }
        data
    }

    /**
     * Mapper mapujący wynik z pojedynczej kolumny na jego wartość.
     * Używany dla zapytań typu `SELECT COUNT(*)`, `SELECT id FROM ...` itp.
     */
    fun <T : Any> SingleValueMapper(kClass: KClass<T>): RowMapper<T?> = RowMapper { rs, _ ->
        val value = valueExtractor.extract(rs, 1)
        if (value == null) return@RowMapper null

        try {
            kClass.cast(value)
        } catch (e: TypeCastException) {
            throw ConversionException(
                messageEnum = ConversionExceptionMessage.VALUE_CONVERSION_FAILED,
                value = value,
                targetType = kClass.simpleName,
                cause = e
            )
        }
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
            } catch (e: Exception) { // To powinien być zawsze ConversionException
                logger.error(e) { e }
                throw e
            }
        }
    }
}