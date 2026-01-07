package org.octavius.report.draganddrop

data class ColumnDragData(
    val columnKey: String,
    val index: Int,
    val sort: Boolean
)