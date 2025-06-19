package org.octavius.form

/**
 * Identyfikuje kolumnę w bazie danych przez tabelę i nazwę pola.
 *
 * @param tableName nazwa tabeli
 * @param fieldName nazwa kolumny
 */
data class ColumnInfo(val tableName: String, val fieldName: String)