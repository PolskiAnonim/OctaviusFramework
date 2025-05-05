package org.octavius.novels.report.filter

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling

abstract class Filter(val columnName: String) {

    abstract fun constructWhereClause(filter: FilterValue<*>): String

    @Composable
    abstract fun RenderFilter(
        currentFilter: FilterValue<*>
    )

    @Composable
    fun NullHandlingPanel(filterValue: FilterValue<*>) {
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
                selected = filterValue.nullHandling.value == NullHandling.Ignore,
                onClick = {
                    filterValue.nullHandling.value = NullHandling.Ignore
                    filterValue.markDirty()
                }
            )
            Text("Ignoruj", modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterValue.nullHandling.value == NullHandling.Include,
                onClick = {
                    filterValue.nullHandling.value = NullHandling.Include
                    filterValue.markDirty()
                }
            )
            Text("Dołącz", modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterValue.nullHandling.value == NullHandling.Exclude,
                onClick = {
                    filterValue.nullHandling.value = NullHandling.Exclude
                    filterValue.markDirty()
                }
            )
            Text("Wyklucz")
        }
    }
}