package org.octavius.novels.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.octavius.novels.form.control.Control

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
        val foreignKeys: List<ForeignKey> = emptyList()
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
}

data class ForeignKey(
    val columnName: String,
    val referencedTable: String,
    var value: Int? = null
)

data class FormControls(
    val controls: Map<String, Control<*>>,
    val order: List<String>,
)

data class ControlResultData(
    val value: Any?,
    val dirty: Boolean
)

data class ControlState<T>(
    val value: MutableState<T?> = mutableStateOf(null),
    val initValue: MutableState<T?> = mutableStateOf(null),
    val error: MutableState<String?> = mutableStateOf(null),
    val dirty: MutableState<Boolean> = mutableStateOf(false),
)