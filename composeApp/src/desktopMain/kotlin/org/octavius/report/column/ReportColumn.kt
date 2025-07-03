package org.octavius.report.column

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.filter.data.FilterData

abstract class ReportColumn(
    val databaseColumnName: String,
    val header: String,
    val width: Float,
    val filterable: Boolean,
    val sortable: Boolean
) {
    fun getFilterData(): FilterData? {
        return if (filterable) {
            createFilterData()
        } else null
    }

    /**
     * Tworzy filtr dla tej kolumny (tylko jeśli filterable = true)
     */
    protected abstract fun createFilterData(): FilterData

    @Composable
    fun RenderHeader(filterData: FilterData?, modifier: Modifier = Modifier) {
        val hasFilter = filterData != null
        var showColumnMenu by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Companion.Center
        ) {
            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = if (hasFilter) {
                    Modifier.Companion.clickable { showColumnMenu = true }
                } else Modifier.Companion
            ) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Companion.Center
                )

                // Wskaźnik aktywnego filtra
                if (hasFilter && filterData.getFilterFragment(databaseColumnName) != null) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = Translations.get("filter.general.active"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.Companion.size(16.dp)
                    )
                }
            }

            // Menu filtra dla kolumny
            if (hasFilter) {
                DropdownMenu(
                    expanded = showColumnMenu,
                    onDismissRequest = { showColumnMenu = false }
                ) {
                    filterData.let { data ->
                        Column(
                            modifier = Modifier.Companion.padding(16.dp)
                        ) {
                            Text(
                                text = Translations.get("filter.general.label") + " $header",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.Companion.padding(bottom = 8.dp)
                            )

                            FilterRenderer(data)

                            // Przycisk wyczyść filtr
                            if (data.getFilterFragment(databaseColumnName) != null) {
                                Row(
                                    modifier = Modifier.Companion.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            filterData.resetFilter()
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

    @Composable
    abstract fun FilterRenderer(data: FilterData)

    @Composable
    abstract fun RenderCell(item: Any?, modifier: Modifier)
}