package org.octavius.report.filter.ui.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.domain.StringFilterDataType
import org.octavius.localization.Translations
import org.octavius.report.filter.data.type.StringFilterData
import org.octavius.report.filter.ui.EnumDropdownMenu
import org.octavius.report.filter.ui.FilterColumnWrapper
import org.octavius.report.filter.ui.FilterModePanel
import org.octavius.report.filter.ui.FilterSpacer
import org.octavius.report.filter.ui.NullHandlingPanel

@Composable
fun StringFilterRenderer(filterData: StringFilterData) {
    FilterColumnWrapper {
        OutlinedTextField(
            value = filterData.value.value,
            onValueChange = { filterData.value.value = it },
            label = { Text(Translations.get("filter.string.value")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (filterData.value.value.isNotEmpty()) {
                    IconButton(onClick = { filterData.value.value = "" }) {
                        Icon(Icons.Default.Clear, Translations.get("filter.general.clear"))
                    }
                }
            }
        )

        FilterSpacer()


        EnumDropdownMenu(
            currentValue = filterData.filterType.value,
            options = StringFilterDataType.entries,
            onValueChange = { filterData.filterType.value = it }
        )


        FilterSpacer()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = filterData.caseSensitive.value,
                onCheckedChange = { filterData.caseSensitive.value = it }
            )
            Text(
                text = Translations.get("filter.string.caseSensitive"),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        FilterSpacer()
        FilterModePanel(filterData)
        FilterSpacer()
        NullHandlingPanel(filterData)
    }
}
