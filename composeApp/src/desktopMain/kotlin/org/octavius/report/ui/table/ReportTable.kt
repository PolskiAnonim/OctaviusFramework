package org.octavius.report.ui.table

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.report.component.ReportState

@Composable
fun ReportTable(
    reportState: ReportState,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {

    val visibleColumns = reportState.columnKeysOrder.filter { reportState.visibleColumns.contains(it) }

    LazyColumn(state = lazyListState, modifier = modifier) {
        item {
            ReportHeaderRow(
                reportState,
                visibleColumns = visibleColumns
            )
        }

        // 2. Wiersze danych
        items(
            items = reportState.data,
        ) { rowData ->
            ReportRow(
                reportState = reportState,
                visibleColumns = visibleColumns,
                rowData = rowData
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

