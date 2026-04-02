package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.octavius.ui.timeline.TimelineBlock

@Composable
internal fun DateNavigationBar(
    selectedDate: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isAutoFilling: Boolean,
    onAutoFillAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }
        Text(
            text = "${selectedDate.day}.${selectedDate.month.number}.${selectedDate.year}",
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = onAutoFillAll,
            enabled = !isAutoFilling,
        ) {
            Text("Auto-fill kategorii")
        }
    }
}

@Composable
internal fun DeleteSlotConfirmationDialog(
    block: TimelineBlock?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usuń slot kategorii") },
        text = {
            val name = block?.label?.takeIf { it.isNotBlank() }
            Text(
                if (name == null) "Czy na pewno chcesz usunąć ten slot?"
                else "Czy na pewno chcesz usunąć slot \"$name\"?"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Usuń")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        },
    )
}
