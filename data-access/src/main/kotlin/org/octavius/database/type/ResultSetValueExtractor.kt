package org.octavius.database.type

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
     */
    private fun extractStandardType(rs: ResultSet, columnIndex: Int, pgTypeName: String): Any? {
        val handler = StandardTypeMappingRegistry.getHandler(pgTypeName)

        // 1. Spróbuj użyć dedykowanej "szybkiej ścieżki", jeśli istnieje.
        handler?.fromResultSet?.let { fastPath ->
            return fastPath(rs, columnIndex)
        }

        // 2. Jeśli nie ma szybkiej ścieżki (handler jest null lub fromResultSet jest null),
        //    użyj uniwersalnej, ale wolniejszej ścieżki opartej na konwersji ze Stringa.
        val rawValue = rs.getString(columnIndex)
        return stringConverter.convert(rawValue, pgTypeName)
    }
}