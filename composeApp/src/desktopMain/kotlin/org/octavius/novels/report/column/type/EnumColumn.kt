package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.domain.EnumWithFormatter
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.filter.type.EnumFilter
import kotlin.reflect.KClass

class EnumColumn<T>(
    columnInfo: ColumnInfo,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val enumClass: KClass<T>,
) : ReportColumn(columnInfo, header, width, filterable, sortable)
        where T : Enum<T>, T : EnumWithFormatter<T> {
    override fun createFilterValue(): FilterValue<*> {
        filter = EnumFilter(name, enumClass)
        return FilterValue.EnumFilter<T>()
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