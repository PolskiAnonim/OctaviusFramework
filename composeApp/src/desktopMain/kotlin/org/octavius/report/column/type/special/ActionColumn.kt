package org.octavius.report.column.type.special

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.ReportActionContext
import org.octavius.report.ReportRowAction

/**
 * Specjalna kolumna, która renderuje menu akcji dla każdego wiersza.
 * Nie jest sortowalna ani filtrowalna.
 */
class ActionColumn(
    private val actions: List<ReportRowAction>
) : SpecialColumn(
    technicalName = "_actions", // Nazwa techniczna
    width = 60.dp
) {
    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        var expanded by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        @Suppress("UNCHECKED_CAST")
        val rowData = item as? Map<String, Any?> ?: return

        Box(
            modifier = modifier.padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = Translations.get("report.actions.menuDescription"),
                    modifier = Modifier.size(18.dp)
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
                            val context = ReportActionContext(rowData, scope)
                            reportAction.action.invoke(context)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}