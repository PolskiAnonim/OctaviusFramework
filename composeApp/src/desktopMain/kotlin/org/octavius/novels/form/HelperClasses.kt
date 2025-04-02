package org.octavius.novels.form

// Klasa pomocnicza do opisania relacji między tabelami
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",  // Puste dla głównej tabeli
    val primaryKey: String = "id"
)

data class ColumnInfo(val tableName: String, val fieldName: String)