package org.octavius.novels.form

// Klasa pomocnicza do opisania relacji między tabelami
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",  // Puste dla głównej tabeli
    val primaryKey: String = "id"
)

data class ColumnInfo(val tableName: String, val fieldName: String)

// SaveOperation.kt
sealed class SaveOperation {
    data class Insert(val data: Map<String, Any?>) : SaveOperation()
    data class Update(val data: Map<String, Any?>) : SaveOperation()
    data class Delete(val data: Any? = null) : SaveOperation()
    data class Skip(val data: Any? = null) : SaveOperation()
}