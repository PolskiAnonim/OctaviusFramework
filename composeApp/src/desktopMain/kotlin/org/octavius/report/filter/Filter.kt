package org.octavius.report.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterData
import org.octavius.report.NullHandling

abstract class Filter(val columnName: String) {

    abstract fun constructWhereClause(filter: FilterData<*>): String

    @Composable
    abstract fun RenderFilter(
        currentFilter: FilterData<*>
    )

    @Composable
    fun NullHandlingPanel(filterData: FilterData<*>) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wartości puste:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            RadioButton(
                selected = filterData.nullHandling.value == NullHandling.Ignore,
                onClick = {
                    filterData.nullHandling.value = NullHandling.Ignore
                    filterData.markDirty()
                }
            )
            Text("Ignoruj", modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.nullHandling.value == NullHandling.Include,
                onClick = {
                    filterData.nullHandling.value = NullHandling.Include
                    filterData.markDirty()
                }
            )
            Text("Dołącz", modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.nullHandling.value == NullHandling.Exclude,
                onClick = {
                    filterData.nullHandling.value = NullHandling.Exclude
                    filterData.markDirty()
                }
            )
            Text("Wyklucz")
        }
    }
}