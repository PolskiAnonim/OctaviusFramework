package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.report.*
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.filter.type.StringFilter

class StringColumn(
    columnInfo: ColumnInfo,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (String?) -> String = { it ?: "" }
) : ReportColumn(columnInfo, header, width, filterable, sortable) {

    override fun createFilterValue(): FilterValue<*> {
        filter = StringFilter(name)
        return FilterValue.TextFilter()
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
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}