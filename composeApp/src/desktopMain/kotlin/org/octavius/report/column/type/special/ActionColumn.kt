package org.octavius.report.column.type.special

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.ReportAction
import org.octavius.report.ReportActionContext
import org.octavius.report.column.ReportColumn
import org.octavius.report.component.ReportState
import org.octavius.report.filter.data.FilterData

/**
 * Specjalna kolumna, która renderuje menu akcji dla każdego wiersza.
 * Nie jest sortowalna ani filtrowalna.
 */
class ActionColumn(
    private val actions: List<ReportAction>,
    private val reportState: ReportState
) : SpecialColumn(
    technicalName = "_actions", // Nazwa techniczna
    width = 0.1f // Wąska kolumna na przycisk
) {
    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        var expanded by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        @Suppress("UNCHECKED_CAST")
        val rowData = item as? Map<String, Any?> ?: return

        Box(
            modifier = modifier.padding(horizontal = 4.dp),
            contentAlignment = Alignment.Companion.Center
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opcje", // TODO: Lepsza translacja
                    modifier = Modifier.Companion.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                actions.forEach { reportAction ->
                    DropdownMenuItem(
                        text = { Text(reportAction.label) },
                        leadingIcon = reportAction.icon?.let {
                            { Icon(it, contentDescription = reportAction.label) }
                        },
                        onClick = {
                            val context = ReportActionContext(rowData, reportState, scope)
                            reportAction.action.invoke(context)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}