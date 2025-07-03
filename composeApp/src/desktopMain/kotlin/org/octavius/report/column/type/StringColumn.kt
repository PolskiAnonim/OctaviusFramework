package org.octavius.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.data.FilterData
import org.octavius.report.filter.data.type.StringFilterData
import org.octavius.report.filter.ui.type.StringFilterRenderer

class StringColumn(
    databaseColumnName: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true
) : ReportColumn(databaseColumnName, header, width, filterable, sortable) {

    override fun createFilterData(): FilterData {
        return StringFilterData()
    }

    @Composable
    override fun FilterRenderer(data: FilterData) {
        StringFilterRenderer(data as StringFilterData)
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val value = item as? String

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}