package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.ColumnState
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.SortDirection
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.filter.type.BooleanFilter

class BooleanColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val trueText: String = "Tak",
    private val falseText: String = "Nie",
    private val showIcon: Boolean = true
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        if (filterable) {
            filter = BooleanFilter(name, falseText, trueText)
            return ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(FilterValue.BooleanFilter())
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
        val value = item as? Boolean

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value != null) {
                if (showIcon) {
                    Icon(
                        imageVector = if (value)
                            Icons.Default.Check
                        else
                            Icons.Default.Close,
                        contentDescription = if (value) trueText else falseText,
                        tint = if (value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = if (value) trueText else falseText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
