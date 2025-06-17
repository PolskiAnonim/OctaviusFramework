package org.octavius.report.column

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

import androidx.compose.ui.Modifier
import org.octavius.report.ColumnState
import org.octavius.report.FilterData
import org.octavius.report.SortDirection
import org.octavius.report.filter.Filter

abstract class ReportColumn(
    val fieldName: String,
    val header: String,
    val width: Float = 1f,
    val filterable: Boolean = false,
    val sortable: Boolean = false
) {
    val name: String get() = fieldName // Dla zachowania kompatybilności

    @Composable
    abstract fun RenderCell(item: Any?, modifier: Modifier)

    var filter: Filter? = null

    open fun initializeState(): ColumnState {
        return if (filterable) {
            ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(createFilterValue())
            )
        } else {
            ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(null)
            )
        }
    }

    protected abstract fun createFilterValue(): FilterData<*>
}