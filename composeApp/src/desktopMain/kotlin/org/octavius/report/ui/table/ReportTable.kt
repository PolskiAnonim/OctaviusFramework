package org.octavius.report.ui.table

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.report.LocalReportContext

@Composable
fun ReportTable(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier
) {
    val reportContext = LocalReportContext.current

    val visibleColumns =
        reportContext.reportState.columnKeysOrder.filter { reportContext.reportState.visibleColumns.contains(it) }

    LazyColumn(state = lazyListState, modifier = modifier) {
        item {
            ReportHeaderRow(
                visibleColumns = visibleColumns
            )
        }

        // 2. Wiersze danych
        items(
            items = reportContext.reportState.data,
        ) { rowData ->
            ReportRow(
                visibleColumns = visibleColumns,
                rowData = rowData
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

