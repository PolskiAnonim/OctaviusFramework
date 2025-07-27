package org.octavius.database

/**
 * Identyfikator kolumny w bazie danych z pełną ścieżką tabelą.kolumna.
 *
 * Data class używana w systemie formularzy do precyzyjnego identyfikowania
 * kolumn w zapytaniach z JOIN. Pozwala różnić kolumny o tej samej nazwie
 * pochodzące z różnych tabel.
 *
 * @param tableName Nazwa tabeli źródłowej kolumny
 * @param fieldName Nazwa kolumny w tabeli
 *
 * @see RowMappers.ColumnInfoMapper
 * @see DatabaseFetcher.fetchEntity
 */
data class ColumnInfo(val tableName: String, val fieldName: String)

/**
 * Definicja relacji między tabelami w zapytaniach JOIN dla formularzy.
 *
 * Data class opisująca jak tabele powinny być połączone w zapytaniach SQL
 * generowanych przez system formularzy. Umożliwia definiowanie złożonych
 * relacji między tabelami główną a tabelami powiązanymi.
 *
 * @param tableName Nazwa tabeli w relacji
 * @param joinCondition Warunek JOIN SQL (pusty string dla tabeli głównej)
 * @param primaryKey Nazwa kolumny klucza głównego (domyślnie "id")
 *
 * Przykład:
 * ```kotlin
 * TableRelation("users") // Tabela główna
 * TableRelation("profiles", "LEFT JOIN profiles p ON p.user_id = users.id")
 * ```
 */
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",
    val primaryKey: String = "id"
)