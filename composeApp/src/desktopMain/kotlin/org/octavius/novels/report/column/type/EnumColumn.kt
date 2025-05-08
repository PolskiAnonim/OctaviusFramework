package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.filter.type.EnumFilter
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

    override fun createFilterValue(): FilterValue<*> {
        filter = EnumFilter(name, enumClass)
        return FilterValue.EnumFilter<E>()
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