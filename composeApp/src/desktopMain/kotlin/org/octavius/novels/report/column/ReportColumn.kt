package org.octavius.novels.report.column

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import org.octavius.novels.report.ColumnState
import org.octavius.novels.report.FilterValue

abstract class ReportColumn(
    val name: String,
    val header: String,
    val width: Float = 1f,
    val filterable: Boolean = false,
    val sortable: Boolean = false
) {
    @Composable
    abstract fun RenderCell(item: Map<String, Any?>, modifier: Modifier)

    @Composable
    abstract fun RenderFilter(
        currentFilter: FilterValue<*>
    )

    abstract fun initializeState(): ColumnState

    abstract fun constructWhereClause(filter: FilterValue<*>): String
}