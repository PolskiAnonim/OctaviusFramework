package org.octavius.novels.report.column

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier
import org.octavius.novels.report.FilterValue

abstract class ReportColumn(
    val name: String,
    val header: String,
    val width: Float = 1f,
    val filtrable: Boolean = false,
    val sortable: Boolean = false
) {
    @Composable
    abstract fun RenderCell(item: Map<String, Any?>, modifier: Modifier = Modifier)

    @Composable
    open fun RenderFilter(
        currentFilter: FilterValue<*>?,
        onFilterChanged: (FilterValue<*>?) -> Unit
    ) {

    }
}