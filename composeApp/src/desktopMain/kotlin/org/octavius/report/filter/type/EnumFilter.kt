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
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Translations
import org.octavius.report.FilterMode
import org.octavius.report.Query
import org.octavius.report.ReportEvent
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.BooleanFilterData
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
            text = Translations.get("filter.enum.selectValues"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Translations.get("filter.enum.selectionMode"))
            RadioButton(
                selected = data.include,
                onClick = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(include = true))) }
            )
            Text(Translations.get("filter.enum.includeSelected"), modifier = Modifier.padding(end = 8.dp))

            RadioButton(
                selected = !data.include,
                onClick = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(include = false))) }
            )
            Text(Translations.get("filter.enum.excludeSelected"))
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
    ): Query? {
        val values = data.values
        val include = data.include
        return when (data.mode) {
            FilterMode.Single -> {
                if (include) {
                    Query("$columnName = ANY(:$columnName)", mapOf(columnName to values))
                } else {
                    Query("$columnName != ALL(:$columnName)", mapOf(columnName to values))
                }
            }
            FilterMode.ListAny -> {
                if (include) {
                    Query("$columnName && :$columnName", mapOf(columnName to values))
                } else {
                    Query("NOT ($columnName && :$columnName)", mapOf(columnName to values))
                }
            }
            FilterMode.ListAll -> {
                if (include) {
                    Query("$columnName @> :$columnName", mapOf(columnName to values))
                } else {
                    Query("NOT ($columnName @> :$columnName)", mapOf(columnName to values))
                }
            }
        }
    }
}