package org.octavius.report.management

data class ColumnDragData(
    val columnKey: String,
    val index: Int,
    val sort: Boolean
)