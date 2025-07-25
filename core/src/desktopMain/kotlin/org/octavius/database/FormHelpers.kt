package org.octavius.database

/**
 * Identyfikuje kolumnę w bazie danych przez tabelę i nazwę pola.
 *
 * @param tableName nazwa tabeli
 * @param fieldName nazwa kolumny
 */
data class ColumnInfo(val tableName: String, val fieldName: String)

/**
 * Opisuje relację między tabelami w zapytaniach JOIN.
 *
 * @param tableName nazwa tabeli
 * @param joinCondition warunek JOIN (pusty dla głównej tabeli)
 * @param primaryKey nazwa kolumny klucza głównego (domyślnie "id")
 */
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",
    val primaryKey: String = "id"
)

/**
 * Sealed class reprezentująca operacje zapisu w bazie danych.
 *
 * Wszystkie operacje zawierają nazwę tabeli i mogą używać foreign key
 * do definiowania relacji między rekordami.
 */
sealed class SaveOperation {
    abstract val tableName: String
    abstract val foreignKeys: List<ForeignKey>
    /**
     * Operacja wstawienia nowego rekordu.
     *
     * @param tableName nazwa tabeli
     * @param data dane do wstawienia (klucz -> wartość)
     * @param foreignKeys lista kluczy obcych
     * @param returningId czy zwrócić ID nowo utworzonego rekordu
     */
    data class Insert(
        override val tableName: String,
        val data: Map<String, Any?>,
        override val foreignKeys: List<ForeignKey> = emptyList(),
        val returningId: Boolean = true
    ) : SaveOperation()

    /**
     * Operacja aktualizacji istniejącego rekordu.
     *
     * @param tableName nazwa tabeli
     * @param data dane do zaktualizowania
     * @param id ID rekordu (może być null jeśli używamy foreign keys)
     * @param foreignKeys lista kluczy obcych do identyfikacji rekordu
     */
    data class Update(
        override val tableName: String,
        val data: Map<String, Any?>,
        val id: Int? = null,
        override val foreignKeys: List<ForeignKey> = emptyList()
    ) : SaveOperation()

    /**
     * Operacja usunięcia rekordu.
     *
     * @param tableName nazwa tabeli
     * @param id ID rekordu do usunięcia (może być null jeśli używamy foreign keys)
     * @param foreignKeys lista kluczy obcych do identyfikacji rekordu
     */
    data class Delete(
        override val tableName: String,
        val id: Int? = null,
        override val foreignKeys: List<ForeignKey> = emptyList()
    ) : SaveOperation()
}

/**
 * Reprezentuje klucz obcy w operacjach bazodanowych.
 *
 * @param columnName nazwa kolumny klucza obcego
 * @param referencedTable nazwa tabeli, do której odnosi się klucz
 * @param value wartość klucza (może być ustawiona później)
 */
data class ForeignKey(
    val columnName: String,
    val referencedTable: String,
    var value: Int? = null
)