package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.ColumnState
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.FilterValue.EnumFilter
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.SortDirection
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.util.Converters.camelToSnakeCase
import kotlin.reflect.KClass

class EnumColumn<E : Enum<*>>(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val enumClass: KClass<out E>,
    private val formatter: (Enum<*>?) -> String = {
        it?.let {
            try {
                val method = it::class.members.find { member -> member.name == "toDisplayString" }
                if (method != null) {
                    method.call(it) as String
                } else {
                    it.toString()
                }
            } catch (e: Exception) {
                it.toString()
            }
        } ?: ""
    }
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        if (filterable) {
            filter = org.octavius.novels.report.filter.type.EnumFilter(name, enumClass)
            return ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(EnumFilter<E>())
            )
        } else {
            return ColumnState(
                mutableStateOf(SortDirection.UNSPECIFIED),
                filtering = mutableStateOf(null)
            )
        }
    }

    @Composable
    override fun RenderCell(item: Any?, modifier: Modifier) {
        @Suppress("UNCHECKED_CAST")
        val value = item as? E

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}