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
import org.octavius.localization.T
import org.octavius.ui.theme.FormSpacing

@Composable
internal fun RepeatableHeader(
    label: String?,
    onAddClick: () -> Unit,
    canAdd: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(FormSpacing.headerHeight),
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
                        contentDescription = T.get("action.add")
                    )
                    Spacer(modifier = Modifier.width(FormSpacing.fieldPaddingHorizontal))
                    Text(T.get("action.add"))
                }
            }
        }
    }
}

@Composable
internal fun RepeatableRowCard(
    row: RepeatableRow,
    index: Int,
    canDelete: Boolean,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = FormSpacing.itemSpacing)
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
                        .padding(FormSpacing.cardPadding)
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
        modifier = Modifier.fillMaxWidth().height(FormSpacing.headerHeight),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onExpandToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = FormSpacing.repeatableRowPadding,
                    vertical = FormSpacing.repeatableHeaderPadding
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = T.get("form.actions.itemLabel", index + 1),
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
                    contentDescription = if (isExpanded) T.get("expandable.collapse") else T.get("expandable.expand"),
                    modifier = Modifier.padding(end = FormSpacing.itemSpacing)
                )

                if (canDelete) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = T.get("action.remove"),
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
internal fun RepeatableRowContent(
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
                        Spacer(modifier = Modifier.height(FormSpacing.controlSpacing))
                    }
                }
            }
        }
    }
}