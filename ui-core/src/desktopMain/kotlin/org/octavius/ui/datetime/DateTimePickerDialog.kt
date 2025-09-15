package org.octavius.ui.datetime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import org.octavius.localization.T
import org.octavius.util.DateTimeAdapter
import org.octavius.util.DateTimeComponent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Prywatny enum do zarządzania krokami w procesie wyboru
private enum class PickerStep { DATE, TIME, OFFSET }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> DateTimePickerDialog(
    adapter: DateTimeAdapter<T>,
    initialValue: T?,
    onDismiss: () -> Unit,
    onConfirm: (T?) -> Unit
) {
    val initialComponents = adapter.getComponents(initialValue)
    val systemDefaultOffset = remember { TimeZone.currentSystemDefault().offsetAt(Clock.System.now()) }

    // Stany tymczasowe, które będą aktualizowane w kolejnych krokach
    var tempDate by remember { mutableStateOf(initialComponents.date) }
    var tempTime by remember { mutableStateOf(initialComponents.time) }
    var tempSeconds by remember { mutableStateOf(initialComponents.seconds?.toString() ?: "0") }
    var tempOffset by remember { mutableStateOf(initialComponents.offset ?: systemDefaultOffset) }

    // Funkcje pomocnicze do nawigacji między krokami
    fun findNextStep(after: PickerStep): PickerStep? {
        return when (after) {
            PickerStep.DATE -> if (adapter.requiredComponents.contains(DateTimeComponent.TIME)) PickerStep.TIME
            else if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) PickerStep.OFFSET
            else null
            PickerStep.TIME -> if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) PickerStep.OFFSET
            else null
            PickerStep.OFFSET -> null
        }
    }

    fun getFirstStep(): PickerStep? {
        if (adapter.requiredComponents.contains(DateTimeComponent.DATE)) return PickerStep.DATE
        if (adapter.requiredComponents.contains(DateTimeComponent.TIME)) return PickerStep.TIME
        if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) return PickerStep.OFFSET
        return null
    }

    // Stan przechowujący aktualny krok
    var currentStep by remember { mutableStateOf(getFirstStep()) }

    fun finish() {
        val finalValue = adapter.buildFromComponents(
            tempDate,
            tempTime?.let { LocalTime(it.hour, it.minute, tempSeconds.toIntOrNull()?.coerceIn(0, 59) ?: 0) },
            tempOffset
        )
        onConfirm(finalValue)
    }

    // Renderuj odpowiednie okno dialogowe w zależności od bieżącego kroku
    when (currentStep) {
        PickerStep.DATE -> {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = tempDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds())
            DatePickerDialog(
                onDismissRequest = onDismiss, // Zamknięcie dialogu z zewnątrz (np. kliknięcie obok)
                dismissButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            tempDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                        }
                        val nextStep = findNextStep(PickerStep.DATE)
                        if (nextStep == null) finish() else currentStep = nextStep
                    }) { Text(T.get("action.ok")) }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(T.get("action.cancel")) }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        PickerStep.TIME -> {
            TimePickerDialog(
                initialTime = tempTime,
                initialSeconds = tempSeconds,
                showSeconds = adapter.requiredComponents.contains(DateTimeComponent.SECONDS),
                onDismiss = onDismiss,
                onConfirm = { time, seconds ->
                    tempTime = time
                    tempSeconds = seconds
                    val nextStep = findNextStep(PickerStep.TIME)
                    if (nextStep == null) finish() else currentStep = nextStep
                }
            )
        }

        PickerStep.OFFSET -> {
            OffsetPickerDialog(
                initialOffset = tempOffset,
                onDismiss = onDismiss,
                onConfirm = { offset ->
                    tempOffset = offset
                    finish() // To zawsze ostatni krok
                }
            )
        }

        null -> {
            // Nic do pokazania, natychmiast zakończ. To obsłuży przypadki, gdy adapter nie ma wymaganych komponentów.
            LaunchedEffect(Unit) {
                finish()
            }
        }
    }
}

// -- Poniżej znajdują się teraz prywatne, pomocnicze komponenty dialogowe dla każdego kroku --

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime?,
    initialSeconds: String,
    showSeconds: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (time: LocalTime, seconds: String) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 12,
        initialMinute = initialTime?.minute ?: 0
    )
    var tempSeconds by remember { mutableStateOf(initialSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.get("datetime.selectTime")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timePickerState)
                if (showSeconds) {
                    OutlinedTextField(
                        value = tempSeconds,
                        onValueChange = { if (it.length <= 2) tempSeconds = it.filter { c -> c.isDigit() } },
                        label = { Text(T.get("datetime.seconds")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime(timePickerState.hour, timePickerState.minute), tempSeconds)
            }) { Text(T.get("action.ok")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(T.get("action.cancel")) }
        }
    )
}

@Composable
private fun OffsetPickerDialog(
    initialOffset: UtcOffset,
    onDismiss: () -> Unit,
    onConfirm: (UtcOffset) -> Unit
) {
    var textOffset by remember { mutableStateOf(initialOffset.toString()) }
    val isError = remember(textOffset) { runCatching { UtcOffset.parse(textOffset) }.isFailure }

    val onConfirmClick = {
        if (!isError) {
            onConfirm(UtcOffset.parse(textOffset))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(T.get("datetime.offset")) },
        text = {
            val commonOffsets by remember { mutableStateOf((-12..14).map { UtcOffset(hours = it) }) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = textOffset,
                    onValueChange = { textOffset = it },
                    label = { Text(T.get("datetime.offset")) },
                    placeholder = { Text("+02:00") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    T.get("datetime.commonOffset"),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start)
                )
                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(commonOffsets) { commonOffset ->
                        Text(
                            text = commonOffset.toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { textOffset = commonOffset.toString() }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick, enabled = !isError) {
                Text(T.get("action.ok"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(T.get("action.cancel")) }
        }
    )
}