package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.report.FilterData
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.filter.type.IntegerFilter

class IntegerColumn(
    fieldName: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (Int?) -> String = { it?.toString() ?: "" }
) : ReportColumn(fieldName, header, width, filterable, sortable) {

    override fun createFilterValue(): FilterData<*> {
        filter = IntegerFilter(name)
        return FilterData.NumberData<Int>()
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val value = item as? Int

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Right
            )
        }
    }
}