package org.octavius.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.domain.EnumWithFormatter
import org.octavius.report.column.ReportColumn
import org.octavius.report.filter.data.FilterData
import org.octavius.report.filter.data.type.EnumFilterData
import org.octavius.report.filter.ui.type.EnumFilterRenderer
import kotlin.reflect.KClass


class EnumColumn<T>(
    databaseColumnName: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    val enumClass: KClass<T>,
) : ReportColumn(databaseColumnName, header, width, filterable, sortable)
        where T : Enum<T>, T : EnumWithFormatter<T> {

    override fun createFilterData(): FilterData {
        return EnumFilterData(enumClass)
    }

    @Composable
    override fun FilterRenderer(data: FilterData) {
        @Suppress("UNCHECKED_CAST")
        EnumFilterRenderer(data as EnumFilterData<T>, enumClass)
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item as? T

        Box(
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value?.toDisplayString() ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}