package org.octavius.report.filter.type

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import org.octavius.data.QueryFragment
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T
import org.octavius.report.FilterMode
import org.octavius.report.ReportEvent
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.EnumFilterData
import kotlin.reflect.KClass

class EnumFilter<E>(private val enumClass: KClass<E>): Filter<EnumFilterData<E>>()
    where E : Enum<E>, E : EnumWithFormatter<E>
{
    override fun createDefaultData(): EnumFilterData<E> = EnumFilterData(enumClass)

    override fun deserializeData(data: JsonObject): EnumFilterData<E> = EnumFilterData.deserialize(data, enumClass)


    @Composable
    override fun RenderFilterUI(onEvent: (ReportEvent) -> Unit, columnKey: String, data: EnumFilterData<E>) {
        val enumValues = remember { enumClass.java.enumConstants.toList() }

        Text(
            text = T.get("filter.enum.selectValues"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(T.get("filter.enum.selectionMode"))
            RadioButton(
                selected = data.include,
                onClick = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(include = true))) }
            )
            Text(T.get("filter.enum.includeSelected"), modifier = Modifier.padding(end = 8.dp))

            RadioButton(
                selected = !data.include,
                onClick = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(include = false))) }
            )
            Text(T.get("filter.enum.excludeSelected"))
        }

        FilterSpacer()

        enumValues.forEach { enumValue ->
            val isSelected = data.values.contains(enumValue)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(values = data.values + enumValue)))
                        } else {
                            onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(values = data.values - enumValue)))
                        }
                    }
                )

                Text(
                    text = enumValue.toDisplayString(),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }

    override fun buildBaseQueryFragment(
        columnName: String,
        data: EnumFilterData<E>
    ): QueryFragment? {
        if (data.values.isEmpty()) return null
        val values = data.values
        val include = data.include
        return when (data.mode) {
            FilterMode.Single -> {
                if (include) {
                    QueryFragment("$columnName = ANY(:$columnName)", mapOf(columnName to values))
                } else {
                    QueryFragment("$columnName != ALL(:$columnName)", mapOf(columnName to values))
                }
            }
            FilterMode.ListAny -> {
                if (include) {
                    QueryFragment("$columnName && :$columnName", mapOf(columnName to values))
                } else {
                    QueryFragment("NOT ($columnName && :$columnName)", mapOf(columnName to values))
                }
            }
            FilterMode.ListAll -> {
                if (include) {
                    QueryFragment(
                        "($columnName @> :$columnName AND $columnName <@ :$columnName)",
                        mapOf(columnName to values)
                    )
                } else {
                    QueryFragment(
                        "NOT ($columnName @> :$columnName AND $columnName <@ :$columnName)",
                        mapOf(columnName to values)
                    )
                }
            }
        }
    }
}