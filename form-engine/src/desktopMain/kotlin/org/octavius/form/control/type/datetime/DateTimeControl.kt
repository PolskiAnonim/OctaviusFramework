package org.octavius.form.control.type.datetime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.octavius.data.contract.ColumnInfo
import org.octavius.form.control.base.*
import org.octavius.form.control.type.datetime.adapter.DateTimeAdapter
import org.octavius.form.control.type.datetime.adapter.DateTimeComponent
import org.octavius.form.control.type.datetime.common.PickerTextField
import org.octavius.form.control.validator.datetime.DateTimeValidator
import org.octavius.localization.T
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
class DateTimeControl<T : Any>(
    private val adapter: DateTimeAdapter<T>,
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: DateTimeValidation<T>? = null,
    actions: List<ControlAction<T>>? = null
) : Control<T>(label, columnInfo, required, dependencies, validationOptions, actions) {
    override val validator: ControlValidator<T> = DateTimeValidator(validationOptions, adapter)

    @Composable
    override fun Display(renderContext: RenderContext, controlState: ControlState<T>, isRequired: Boolean) {
        val scope = rememberCoroutineScope()
        val currentValue = controlState.value.value

        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var showOffsetPicker by remember { mutableStateOf(false) }

        var tempDate by remember { mutableStateOf(adapter.getComponents(currentValue).date) }
        var tempTime by remember { mutableStateOf(adapter.getComponents(currentValue).time) }
        var tempSeconds by remember { mutableStateOf(adapter.getComponents(currentValue).seconds?.toString() ?: "0") }

        // Domyślna wartość offsetu: istniejąca wartość, lub offset systemowy
        val systemDefaultOffset by remember {
            mutableStateOf(ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()))
        }
        var tempOffset by remember {
            mutableStateOf(
                adapter.getComponents(currentValue).offset?.toString() ?: systemDefaultOffset.toString()
            )
        }


        fun startPickerFlow() {
            if (adapter.requiredComponents.contains(DateTimeComponent.DATE)) {
                showDatePicker = true
            } else if (adapter.requiredComponents.contains(DateTimeComponent.TIME)) {
                showTimePicker = true
            }
        }

        fun confirmAndBuild() {
            val finalValue = adapter.buildFromComponents(
                tempDate,
                tempTime?.let {
                    LocalTime(
                        hour = it.hour,
                        minute = it.minute,
                        second = tempSeconds.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    )
                },
                // Używamy runCatching, aby uniknąć crashu przy błędnym formacie
                runCatching { ZoneOffset.of(tempOffset) }.getOrNull()
            )
            controlState.value.value = finalValue
            executeActions(renderContext, finalValue, scope)
        }

        PickerTextField(
            value = adapter.format(currentValue),
            onClick = { startPickerFlow() },
            onClear = if (!isRequired) {
                {
                    controlState.value.value = null
                    executeActions(renderContext, null, scope)
                }
            } else null,
            isRequired = isRequired
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = adapter.getEpochMillis(currentValue)
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            tempDate = adapter.dateFromEpochMillis(it)
                            showDatePicker = false
                            if (adapter.requiredComponents.contains(DateTimeComponent.TIME)) {
                                showTimePicker = true
                            } else {
                                confirmAndBuild()
                            }
                        } ?: run { showDatePicker = false }
                    }) { Text(T.get("action.select")) }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(T.get("action.cancel")) } }
            ) { androidx.compose.material3.DatePicker(state = datePickerState) }
        }

        if (showTimePicker) {
            val initialComponents = adapter.getComponents(currentValue)
            val timePickerState = rememberTimePickerState(
                initialHour = initialComponents.time?.hour ?: 12,
                initialMinute = initialComponents.time?.minute ?: 0
            )
            // Resetuj sekundy przy otwarciu pickera, jeśli nie ma ich w aktualnej wartości
            LaunchedEffect(Unit) {
                tempSeconds = initialComponents.seconds?.toString() ?: "0"
            }
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        tempTime = LocalTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                        if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) {
                            // Ustaw domyślny offset systemowy, jeśli żaden nie jest jeszcze wybrany
                            if (adapter.getComponents(currentValue).offset == null) {
                                tempOffset = systemDefaultOffset.toString()
                            }
                            showOffsetPicker = true
                        } else {
                            confirmAndBuild()
                        }
                    }) { Text(T.get("action.ok")) }
                },
                dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(T.get("action.cancel")) } },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = timePickerState)
                        if (adapter.requiredComponents.contains(DateTimeComponent.SECONDS)) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = tempSeconds,
                                onValueChange = { if (it.length <= 2) tempSeconds = it.filter { c -> c.isDigit() } },
                                label = { Text(T.get("form.datetime.seconds")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            )
        }

        if (showOffsetPicker) {
            // Lista popularnych offsetów (tylko pełne godziny dla prostoty)
            val commonOffsets by remember {
                mutableStateOf((-12..14).map { ZoneOffset.ofHours(it) })
            }

            AlertDialog(
                onDismissRequest = { showOffsetPicker = false },
                title = { Text(T.get("form.datetime.offset")) }, // Dodany tytuł dla jasności
                confirmButton = {
                    TextButton(onClick = {
                        // Sprawdzamy, czy wpisany tekst jest poprawnym offsetem przed zamknięciem
                        if (runCatching { ZoneOffset.of(tempOffset) }.isSuccess) {
                            showOffsetPicker = false
                            confirmAndBuild()
                        }
                    }) { Text(T.get("action.ok")) }
                },
                dismissButton = { TextButton(onClick = { showOffsetPicker = false }) { Text(T.get("action.cancel")) } },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = tempOffset,
                            onValueChange = { tempOffset = it },
                            label = { Text(T.get("form.datetime.offset")) },
                            placeholder = { Text("+02:00") },
                            singleLine = true,
                            isError = runCatching { ZoneOffset.of(tempOffset) }.isFailure,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            T.get("form.datetime.commonOffset"),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        // Używamy LazyColumn dla przewijanej listy
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(commonOffsets) { offset ->
                                Text(
                                    text = offset.toString(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { tempOffset = offset.toString() }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}