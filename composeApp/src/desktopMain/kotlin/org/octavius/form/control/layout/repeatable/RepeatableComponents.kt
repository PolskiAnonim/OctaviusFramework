package org.octavius.form.control.layout.repeatable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.form.component.FormState
import org.octavius.form.control.base.Control
import org.octavius.form.control.type.repeatable.RepeatableRow

@Composable
fun RepeatableHeader(
    label: String?,
    onAddClick: () -> Unit,
    canAdd: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Zawsze zajmuj miejsce na przycisk, ale pokaż tylko gdy można dodać
        Box(
            modifier = Modifier.width(150.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (canAdd) {
                FilledTonalButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dodaj")
                }
            }
        }
    }
}

@Composable
fun RepeatableRowCard(
    row: RepeatableRow,
    index: Int,
    canDelete: Boolean,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        var isExpanded by remember { mutableStateOf(true) }

        Column {
            RepeatableRowHeader(
                index = index,
                isExpanded = isExpanded,
                onExpandToggle = { isExpanded = !isExpanded },
                canDelete = canDelete,
                onDelete = onDelete
            )

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun RepeatableRowHeader(
    index: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onExpandToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Element ${index + 1}",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(96.dp) // Stała szerokość na ikonę + przycisk
            ) {
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                    modifier = Modifier.padding(end = 8.dp)
                )

                if (canDelete) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Usuń",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepeatableRowContent(
    row: RepeatableRow,
    controlName: String,
    rowOrder: List<String>,
    rowControls: Map<String, Control<*>>,
    formState: FormState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        rowOrder.forEach { fieldName ->
            rowControls[fieldName]?.let { control ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val state = formState.getControlState(hierarchicalName)
                if (state != null) {
                    control.Render(
                        controlName = hierarchicalName,
                        controlState = state
                    )

                    if (fieldName != rowOrder.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}