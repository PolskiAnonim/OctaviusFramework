package org.octavius.report.filter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Translations
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.filter.data.FilterData

@Composable
fun NullHandlingPanel(filterData: FilterData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Translations.get("filter.null.values"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        RadioButton(
            selected = filterData.nullHandling.value == NullHandling.Ignore,
            onClick = {
                filterData.nullHandling.value = NullHandling.Ignore
            }
        )
        Text(Translations.get("filter.null.ignore"), modifier = Modifier.padding(end = 12.dp))

        RadioButton(
            selected = filterData.nullHandling.value == NullHandling.Include,
            onClick = {
                filterData.nullHandling.value = NullHandling.Include
            }
        )
        Text(Translations.get("filter.null.include"), modifier = Modifier.padding(end = 12.dp))

        RadioButton(
            selected = filterData.nullHandling.value == NullHandling.Exclude,
            onClick = {
                filterData.nullHandling.value = NullHandling.Exclude
            }
        )
        Text(Translations.get("filter.null.exclude"))
    }
}

@Composable
fun FilterModePanel(filterData: FilterData) {
    if (filterData.mode.value == FilterMode.Single) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Translations.get("filter.mode"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        RadioButton(
            selected = filterData.mode.value == FilterMode.ListAny,
            onClick = {
                filterData.mode.value = FilterMode.ListAny
            }
        )
        Text(FilterMode.ListAny.toDisplayString(), modifier = Modifier.padding(end = 12.dp))

        RadioButton(
            selected = filterData.mode.value == FilterMode.ListAll,
            onClick = {
                filterData.mode.value = FilterMode.ListAll
            }
        )
        Text(FilterMode.ListAll.toDisplayString())
    }
}

@Composable
fun FilterColumnWrapper(content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : EnumWithFormatter<T>> EnumDropdownMenu(
    currentValue: T,
    options: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = currentValue.toDisplayString(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toDisplayString()) },
                    onClick = {
                        expanded = false
                        onValueChange(option)
                    },
                    trailingIcon = if (currentValue == option) {
                        { Icon(Icons.Default.Check, null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun FilterSpacer() {
    Spacer(modifier = Modifier.height(8.dp))
}
