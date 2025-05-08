package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.*
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.filter.type.IntegerFilter

class IntegerColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val formatter: (Int?) -> String = { it?.toString() ?: "" }
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        if (filterable) {
            filter = IntegerFilter(name)
            return ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(FilterValue.NumberFilter<Int>())
            )
        } else {
            return ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(null)
            )
        }
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