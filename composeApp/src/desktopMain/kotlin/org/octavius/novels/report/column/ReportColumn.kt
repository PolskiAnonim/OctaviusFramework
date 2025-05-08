package org.octavius.novels.report.column

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import org.octavius.novels.report.ColumnState
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.filter.Filter

abstract class ReportColumn(
    val name: String,
    val header: String,
    val width: Float = 1f,
    val filterable: Boolean = false,
    val sortable: Boolean = false
) {
    @Composable
    abstract fun RenderCell(item: Any?, modifier: Modifier)

    var filter: Filter? = null

    abstract fun initializeState(): ColumnState
}