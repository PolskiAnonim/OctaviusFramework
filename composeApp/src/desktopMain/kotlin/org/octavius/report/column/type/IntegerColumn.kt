package org.octavius.report.column.type

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.octavius.report.CellRendererUtils
import org.octavius.report.ColumnWidth
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.data.FilterData
import org.octavius.report.filter.data.type.NumberFilterData
import org.octavius.report.filter.ui.type.IntegerFilterRenderer

class IntegerColumn(
    databaseColumnName: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (Int?) -> String = { it?.toString() ?: "" }
) : ReportColumn(databaseColumnName, header, ColumnWidth.Flexible(width), filterable, sortable) {

    override fun createFilterData(): FilterData {
        return NumberFilterData(Int::class)
    }

    @Composable
    override fun FilterRenderer(data: FilterData) {
        @Suppress("UNCHECKED_CAST")
        IntegerFilterRenderer(data as NumberFilterData<Int>)
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val value = item as? Int

        CellRendererUtils.StandardCellWrapper(
            modifier = modifier,
            alignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        }
    }
}