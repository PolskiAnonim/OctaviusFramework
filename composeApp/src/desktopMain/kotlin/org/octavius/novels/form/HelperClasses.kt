package org.octavius.novels.form

// Klasa pomocnicza do opisania relacji między tabelami
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",  // Puste dla głównej tabeli
    val primaryKey: String = "id"
)

data class ColumnInfo(val tableName: String, val fieldName: String)

sealed class SaveOperation {
    abstract val tableName: String

    data class Insert(
        override val tableName: String,
        val data: Map<String, ControlResultData>,
        val id: Int? = null
    ) : SaveOperation()

    data class Update(
        override val tableName: String,
        val data: Map<String, ControlResultData>,
        val id: Int
    ) : SaveOperation()

    data class Delete(
        override val tableName: String,
        val id: Int
    ) : SaveOperation()

    data class Skip(
        override val tableName: String
    ) : SaveOperation()
}