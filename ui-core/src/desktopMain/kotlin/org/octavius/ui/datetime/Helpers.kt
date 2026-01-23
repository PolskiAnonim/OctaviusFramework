package org.octavius.ui.datetime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Renderuje pole tekstowe, które wygląda jak standardowy OutlinedTextField,
 * ale jest nieedytowalne i służy jako przycisk do otwierania okna dialogowego.
 * Jest teraz uniwersalne i może być używane w formularzach oraz filtrach.
 *
 * @param value Aktualnie wyświetlana wartość tekstowa.
 * @param onClick Lambda wywoływana po kliknięciu.
 * @param modifier Modyfikator do zastosowania na całym komponencie.
 * @param label Opcjonalna etykieta dla pola.
 * @param onClear Opcjonalna lambda do czyszczenia wartości (pokazuje przycisk 'X').
 * @param isRequired Wskazuje, czy pole jest wymagane (wpływa na tekst zastępczy).
 */
@Composable
fun PickerTextField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    onClear: (() -> Unit)? = null,
    isRequired: Boolean = false
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value.ifEmpty { if (isRequired) Tr.Datetime.select() else Tr.Datetime.notSet() },
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(), // Pole zawsze wypełnia dostępną przestrzeń od rodzica (Box)
            label = label?.let { { Text(it) } },
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
                            contentDescription = Tr.Action.clear()
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
        title = { Text(Tr.Interval.title()) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IntervalInputField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = Tr.Interval.hours()
                )
                Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                IntervalInputField(
                    value = minutes,
                    onValueChange = { minutes = it },
                    label = Tr.Interval.minutes()
                )
                Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                IntervalInputField(
                    value = seconds,
                    onValueChange = { seconds = it },
                    label = Tr.Interval.seconds()
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
                Text(Tr.Action.ok())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Tr.Action.cancel())
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