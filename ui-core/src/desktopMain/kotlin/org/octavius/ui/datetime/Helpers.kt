package org.octavius.ui.datetime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.octavius.localization.T
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Renderuje pole tekstowe, które wygląda jak standardowy OutlinedTextField,
 * ale jest nieedytowalne i służy jako przycisk do otwierania okna dialogowego.
 */
@Composable
fun PickerTextField(
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    isRequired: Boolean
) {
    Box {
        OutlinedTextField(
            value = value.ifEmpty { if (isRequired) T.get("form.datetime.select") else T.get("form.datetime.notSet") },
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (onClear != null && value.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = T.get("action.clear")
                        )
                    }
                }
            },
            // Zmieniamy kolory, aby wyglądało na nieaktywne, ale wciąż czytelne
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = OutlinedTextFieldDefaults.colors().unfocusedTextColor,
                disabledBorderColor = OutlinedTextFieldDefaults.colors().unfocusedContainerColor,
                disabledLeadingIconColor = OutlinedTextFieldDefaults.colors().unfocusedLeadingIconColor,
                disabledTrailingIconColor = OutlinedTextFieldDefaults.colors().unfocusedTrailingIconColor,
            )
        )
        // Nakładamy na wierzch klikalny, przezroczysty box
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

/**
 * Okno dialogowe do wprowadzania interwału czasowego (godziny, minuty, sekundy).
 */
@Composable
fun IntervalPickerDialog(
    initialValue: Duration?,
    onDismiss: () -> Unit,
    onConfirm: (Duration) -> Unit
) {
    val (h, m, s) = initialValue?.toComponents { hours, minutes, seconds, _ ->
        Triple(hours.toString(), minutes.toString(), seconds.toString())
    } ?: Triple("", "", "")

    var hours by remember { mutableStateOf(h) }
    var minutes by remember { mutableStateOf(m) }
    var seconds by remember { mutableStateOf(s) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.get("form.interval.title")) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IntervalInputField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = T.get("form.interval.hours")
                )
                Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                IntervalInputField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = T.get("form.interval.minutes")
                )
                Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                IntervalInputField(
                    value = seconds,
                    onValueChange = { seconds = it },
                    label = T.get("form.interval.seconds")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = (hours.toLongOrNull() ?: 0).hours +
                            (minutes.toLongOrNull() ?: 0).minutes +
                            (seconds.toLongOrNull() ?: 0).seconds
                    onConfirm(duration)
                }
            ) {
                Text(T.get("action.ok"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(T.get("action.cancel"))
            }
        }
    )
}

@Composable
private fun RowScope.IntervalInputField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.weight(1f)
    )
}