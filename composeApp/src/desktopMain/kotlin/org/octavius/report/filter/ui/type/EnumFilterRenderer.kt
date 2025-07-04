package org.octavius.report.filter.ui.type

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
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Translations
import org.octavius.report.filter.data.type.EnumFilterData
import org.octavius.report.filter.ui.FilterColumnWrapper
import org.octavius.report.filter.ui.FilterModePanel
import org.octavius.report.filter.ui.FilterSpacer
import org.octavius.report.filter.ui.NullHandlingPanel
import kotlin.reflect.KClass

@Composable
fun <E : Enum<E>> EnumFilterRenderer(filterData: EnumFilterData<E>, enumClass: KClass<E>) {
    val enumValues = remember { enumClass.java.enumConstants.toList() }

    FilterColumnWrapper {
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
                selected = filterData.include.value,
                onClick = { filterData.include.value = true }
            )
            Text(Translations.get("filter.enum.includeSelected"), modifier = Modifier.padding(end = 8.dp))

            RadioButton(
                selected = !filterData.include.value,
                onClick = { filterData.include.value = false }
            )
            Text(Translations.get("filter.enum.excludeSelected"))
        }

        FilterSpacer()

        enumValues.forEach { enumValue ->
            val isSelected = filterData.values.contains(enumValue)

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
                            filterData.values.add(enumValue)
                        } else {
                            filterData.values.remove(enumValue)
                        }
                    }
                )

                Text(
                    text = if (enumValue is EnumWithFormatter<*>) {
                        enumValue.toDisplayString()
                    } else {
                        enumValue.toString()
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        FilterSpacer()
        FilterModePanel(filterData)
        FilterSpacer()
        NullHandlingPanel(filterData)
    }
}