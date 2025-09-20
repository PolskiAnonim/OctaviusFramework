package org.octavius.data.contract

/**
 * Opakowuje wartość, aby jawnie określić docelowy typ PostgreSQL.
 *
 * Powoduje dodanie rzutowania typu (`::pgType`) do wygenerowanego fragmentu SQL.
 * Przydatne do obsługi niejednoznaczności typów, np. przy tablicach.
 *
 * @param value Wartość do osadzenia w zapytaniu (należy unikać data class gdzie jest to dodawane automatycznie!).
 * @param pgType Nazwa typu PostgreSQL, na który wartość ma być rzutowana (np. "text[]", "jsonb").
 */
data class PgTyped(val value: Any?, val pgType: String)


/**
 * Identyfikuje kolumnę w bazie danych, uwzględniając nazwę tabeli.
 *
 * Używane do rozróżniania kolumn o tych samych nazwach w zapytaniach z `JOIN`.
 *
 * @param tableName Nazwa tabeli źródłowej.
 * @param fieldName Nazwa kolumny.
 */
data class ColumnInfo(val tableName: String, val fieldName: String)
