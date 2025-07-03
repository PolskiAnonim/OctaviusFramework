package org.octavius.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.data.FilterData
import org.octavius.report.filter.data.type.BooleanFilterData
import org.octavius.report.filter.ui.type.BooleanFilterRenderer


class BooleanColumn(
    databaseColumnName: String,
    header: String,
    width: Float = 1f,
    filterable: Boolean = false,
    sortable: Boolean = true,
    private val trueText: String = Translations.get("report.column.boolean.true"),
    private val falseText: String = Translations.get("report.column.boolean.false"),
    private val showIcon: Boolean = true
) : ReportColumn(databaseColumnName, header, width, filterable, sortable) {
    @Composable
    override fun FilterRenderer(data: FilterData) {
        BooleanFilterRenderer(data as BooleanFilterData, trueText, falseText)
    }

    override fun createFilterData(): FilterData {
        return BooleanFilterData()
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        val value = item as? Boolean

        Box(
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value != null) {
                if (showIcon) {
                    Icon(
                        imageVector = if (value) Icons.Default.Check
                        else Icons.Default.Close,
                        contentDescription = if (value) trueText else falseText,
                        tint = if (value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = if (value) trueText else falseText, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}