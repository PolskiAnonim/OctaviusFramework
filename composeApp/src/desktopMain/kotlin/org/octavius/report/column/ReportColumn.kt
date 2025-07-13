package org.octavius.report.column

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
import org.octavius.localization.Translations
import org.octavius.report.ColumnWidth
import org.octavius.report.ReportEvent
import org.octavius.report.component.ReportState
import org.octavius.report.filter.Filter
import org.octavius.report.filter.data.FilterData

abstract class ReportColumn(
    val databaseColumnName: String,
    val header: String,
    val width: ColumnWidth,
    val filterable: Boolean,
    val sortable: Boolean
) {

    val filter: Filter<*>? by lazy {
        if (filterable) createFilter() else null
    }

    abstract fun createFilter(): Filter<*>

    protected open fun createFilterData(filter: Filter<*>): FilterData {
        return filter.createDefaultData()
    }

    fun createFilterAndFilterData(): FilterData? {
        return filter?.let { createFilterData(it) }
    }

    @Composable
    fun RenderHeader(
        onEvent: (ReportEvent) -> Unit,
        columnKey: String,
        reportState: ReportState,
        modifier: Modifier = Modifier
    ) {
        val filterData = reportState.filterData[columnKey]

        var showColumnMenu by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = if (filterData != null) {
                    Modifier.clickable { showColumnMenu = true }
                } else Modifier
            ) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Companion.Center
                )

                // Wskaźnik aktywnego filtra
                if (filterData?.isActive() ?: false) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = Translations.get("filter.general.active"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.Companion.size(16.dp)
                    )
                }
            }

            // Menu filtra dla kolumny
            if (filterData != null) {
                DropdownMenu(
                    expanded = showColumnMenu,
                    onDismissRequest = { showColumnMenu = false }
                ) {
                    Column(
                        modifier = Modifier.Companion.padding(16.dp)
                    ) {
                        Text(
                            text = Translations.get("filter.general.label") + " $header",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.Companion.padding(bottom = 8.dp)
                        )

                        RenderFilter(onEvent, columnKey, filter!!, filterData)

                        // Przycisk wyczyść filtr
                        if (filterData.isActive()) {
                            Row(
                                modifier = Modifier.Companion.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onEvent.invoke(ReportEvent.ClearFilter(columnKey))
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

    /**
     * Funkcja renderująca filtr - zmienia * w T dla typu generycznego
     * Wymagana aby zadowolić kompilator
     */
    @Composable
    private fun <T : FilterData> RenderFilter(
        onEvent: (ReportEvent) -> Unit,
        columnKey: String,
        filter: Filter<T>,
        data: FilterData
    ) {
        @Suppress("UNCHECKED_CAST")
        val specificData = data as T

        filter.Render(onEvent, columnKey, specificData)
    }

    @Composable
    abstract fun RenderCell(item: Any?, modifier: Modifier)
}