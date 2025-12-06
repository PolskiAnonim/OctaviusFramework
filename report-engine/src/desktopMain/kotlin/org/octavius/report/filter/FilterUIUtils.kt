package org.octavius.report.filter

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.domain.EnumWithFormatter

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
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
