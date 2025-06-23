package org.octavius.report.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterData
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.Filter
import org.octavius.localization.Translations

@Composable
fun ReportColumnHeader(
    columnKey: String,
    column: ReportColumn,
    filter: Filter?,
    filterData: FilterData<*>?,
    reportState: ReportState,
    modifier: Modifier = Modifier
) {
    val hasFilter = filter != null
    var showColumnMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = if (hasFilter) {
                Modifier.clickable { showColumnMenu = true }
            } else Modifier
        ) {
            Text(
                text = column.header,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            // Wskaźnik aktywnego filtra
            if (hasFilter && filterData?.isActive() == true) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = Translations.get("filter.general.active"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Menu filtra dla kolumny
        if (hasFilter && filter != null) {
            DropdownMenu(
                expanded = showColumnMenu,
                onDismissRequest = { showColumnMenu = false }
            ) {
                filterData?.let { data ->
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = Translations.get("filter.general.label") + " ${column.header}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        filter.RenderFilter(data)

                        // Przycisk wyczyść filtr
                        if (data.isActive()) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val currentFilterData = reportState.filterValues.value[columnKey]!!
                                        currentFilterData.reset()
                                        reportState.currentPage.value = 0
                                        showColumnMenu = false
                                    }
                                ) {
                                    Text(Translations.get("filter.general.clear"))
                                }

                                Button(
                                    onClick = { showColumnMenu = false }
                                ) {
                                    Text(Translations.get("action.close"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}