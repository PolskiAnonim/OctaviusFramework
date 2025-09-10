package org.octavius.report.filter.type

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonObject
import org.octavius.localization.T
import org.octavius.report.DateTimeFilterDataType
import org.octavius.report.FilterMode
import org.octavius.report.Query
import org.octavius.report.ReportEvent
import org.octavius.report.filter.EnumDropdownMenu
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.DateTimeFilterData
import org.octavius.util.DateTimeAdapter
import org.octavius.util.DateTimeComponent
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
class DateTimeFilter<T : Any>(
    private val kClass: KClass<T>,
    private val adapter: DateTimeAdapter<T>
) : Filter<DateTimeFilterData<T>>() {

    override fun createDefaultData(): DateTimeFilterData<T> = DateTimeFilterData(kClass, adapter)

    override fun deserializeData(data: JsonObject): DateTimeFilterData<T> = DateTimeFilterData.deserialize(data, kClass, adapter)

    @Composable
    override fun RenderFilterUI(onEvent: (ReportEvent) -> Unit, columnKey: String, data: DateTimeFilterData<T>) {
        // Ujednolicone stany widoczności pickerów
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var showOffsetPicker by remember { mutableStateOf(false) }

        // Śledzi, czy wybieramy wartość dla 'minValue' (lub pojedynczej 'value') czy 'maxValue'
        var isPickingMinOrValue by remember { mutableStateOf(true) }

        // Tymczasowe stany do przechowywania komponentów daty/czasu podczas wyboru
        var tempDate by remember { mutableStateOf<LocalDate?>(null) }
        var tempTime by remember { mutableStateOf<LocalTime?>(null) }
        var tempSeconds by remember { mutableStateOf("0") }
        var tempOffset by remember { mutableStateOf<ZoneOffset?>(null) }

        val systemDefaultOffset by remember {
            mutableStateOf(ZoneId.systemDefault().rules.getOffset(java.time.Instant.now()))
        }

        // Funkcja do zbudowania i aktualizacji danych filtra po wybraniu wszystkich komponentów
        fun confirmAndBuildValue() {
            val finalTime = tempTime?.let {
                LocalTime(
                    hour = it.hour,
                    minute = it.minute,
                    second = tempSeconds.toIntOrNull()?.coerceIn(0, 59) ?: 0
                )
            }
            // Używamy runCatching do parsowania offsetu, zwracamy null w przypadku błędu
            val finalOffset = runCatching { tempOffset?.toString()?.let { ZoneOffset.of(it) } }.getOrNull()

            val newDateTimeValue = adapter.buildFromComponents(tempDate, finalTime, finalOffset)

            if (isPickingMinOrValue) {
                if (data.filterType == DateTimeFilterDataType.Range) {
                    onEvent(ReportEvent.FilterChanged(columnKey, data.copy(minValue = newDateTimeValue)))
                } else {
                    onEvent(ReportEvent.FilterChanged(columnKey, data.copy(value = newDateTimeValue)))
                }
            } else {
                onEvent(ReportEvent.FilterChanged(columnKey, data.copy(maxValue = newDateTimeValue)))
            }

            // Resetuj widoczność pickerów
            showDatePicker = false
            showTimePicker = false
            showOffsetPicker = false
        }

        // Funkcja inicjująca przepływ wyboru dla konkretnej wartości (min/value lub max)
        fun startPickerFlowFor(forMinOrValue: Boolean) {
            isPickingMinOrValue = forMinOrValue

            val initialValue = if (forMinOrValue) (data.value ?: data.minValue) else data.maxValue
            val components = adapter.getComponents(initialValue)

            tempDate = components.date
            tempTime = components.time
            tempSeconds = components.seconds?.toString() ?: "0"
            tempOffset = components.offset ?: systemDefaultOffset // Inicjalizuj systemowym offsetem, jeśli brak

            if (adapter.requiredComponents.contains(DateTimeComponent.DATE)) {
                showDatePicker = true
            } else if (adapter.requiredComponents.contains(DateTimeComponent.TIME)) {
                showTimePicker = true
            } else if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) {
                showOffsetPicker = true
            } else {
                // Jeśli żadne komponenty nie są wymagane, od razu zbuduj wartość
                confirmAndBuildValue()
            }
        }

        EnumDropdownMenu(
            currentValue = data.filterType,
            options = DateTimeFilterDataType.entries,
            onValueChange = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(filterType = it))) }
        )

        FilterSpacer()

        // UI dla pojedynczej wartości lub zakresu
        if (data.filterType == DateTimeFilterDataType.Range) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = adapter.format(data.minValue),
                    onValueChange = { /* tylko do odczytu */ },
                    label = { Text(T.get("filter.datetime.from")) },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startPickerFlowFor(true) }) {
                            Icon(Icons.Default.DateRange, contentDescription = T.get("filter.datetime.selectDateTime"))
                        }
                    }
                )
                OutlinedTextField(
                    value = adapter.format(data.maxValue),
                    onValueChange = { /* tylko do odczytu */ },
                    label = { Text(T.get("filter.datetime.to")) },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { startPickerFlowFor(false) }) {
                            Icon(Icons.Default.DateRange, contentDescription = T.get("filter.datetime.selectDateTime"))
                        }
                    }
                )
            }
        } else {
            OutlinedTextField(
                value = adapter.format(data.value),
                onValueChange = { /* tylko do odczytu */ },
                label = { Text(T.get("filter.datetime.value")) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { startPickerFlowFor(true) }) {
                        Icon(Icons.Default.DateRange, contentDescription = T.get("filter.datetime.selectDateTime"))
                    }
                }
            )
        }

        // --- Date Picker Dialog ---
        if (showDatePicker) {
            val initialMillis = if (isPickingMinOrValue) {
                (data.value ?: data.minValue)?.let { adapter.getEpochMillis(it) }
            } else {
                data.maxValue?.let { adapter.getEpochMillis(it) }
            }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialMillis
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                dismissButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            tempDate = adapter.dateFromEpochMillis(it)
                            showDatePicker = false
                            if (adapter.requiredComponents.contains(DateTimeComponent.TIME)) {
                                showTimePicker = true
                            } else if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) {
                                showOffsetPicker = true
                            } else {
                                confirmAndBuildValue()
                            }
                        } ?: run { showDatePicker = false } // Jeśli data nie została wybrana, po prostu zamknij
                    }) { Text(T.get("action.select")) }
                },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text(T.get("action.cancel")) } }
            ) { DatePicker(state = datePickerState) }
        }

        // --- Time Picker Dialog ---
        if (showTimePicker) {
            // Inicjalizuj stan time pickera z tempTime, lub domyślny
            val initialHour = tempTime?.hour ?: 12
            val initialMinute = tempTime?.minute ?: 0
            val timePickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)

            // Inicjalizuj pole tekstowe sekund przy otwieraniu pickera
            LaunchedEffect(Unit) {
                tempSeconds = tempTime?.second?.toString() ?: "0"
            }

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        tempTime = LocalTime(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                        if (adapter.requiredComponents.contains(DateTimeComponent.OFFSET)) {
                            // Upewnij się, że tempOffset jest ustawiony na systemDefaultOffset, jeśli jest null przed otwarciem pickera offsetu
                            if (tempOffset == null) {
                                tempOffset = systemDefaultOffset
                            }
                            showOffsetPicker = true
                        } else {
                            confirmAndBuildValue()
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

        // --- Offset Picker Dialog ---
        if (showOffsetPicker) {
            val commonOffsets by remember {
                mutableStateOf((-12..14).map { ZoneOffset.ofHours(it) })
            }
            // Użyj osobnego mutableStateOf dla wartości pola tekstowego, inicjalizowanego z tempOffset
            var currentOffsetInput by remember(tempOffset) { mutableStateOf(tempOffset?.toString() ?: systemDefaultOffset.toString()) }

            AlertDialog(
                onDismissRequest = { showOffsetPicker = false },
                title = { Text(T.get("form.datetime.offset")) },
                confirmButton = {
                    TextButton(onClick = {
                        runCatching { ZoneOffset.of(currentOffsetInput) }.getOrNull()?.let {
                            tempOffset = it
                            showOffsetPicker = false
                            confirmAndBuildValue()
                        }
                    }) { Text(T.get("action.ok")) }
                },
                dismissButton = { TextButton(onClick = { showOffsetPicker = false }) { Text(T.get("action.cancel")) } },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = currentOffsetInput,
                            onValueChange = { currentOffsetInput = it },
                            label = { Text(T.get("form.datetime.offset")) },
                            placeholder = { Text("+02:00") },
                            singleLine = true,
                            isError = runCatching { ZoneOffset.of(currentOffsetInput) }.isFailure,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(T.get("form.datetime.commonOffset"), modifier = Modifier.align(Alignment.Start))
                        LazyColumn(modifier = Modifier.height(150.dp)) {
                            items(commonOffsets) { offset ->
                                Text(
                                    text = offset.toString(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentOffsetInput = offset.toString() }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    override fun buildBaseQueryFragment(columnName: String, data: DateTimeFilterData<T>): Query? {
        val value = data.value
        val min = data.minValue
        val max = data.maxValue
        val filterType = data.filterType

        return when (data.mode) {
            FilterMode.Single -> buildSingleDateTimeQuery(columnName, value, min, max, filterType)
            FilterMode.ListAny -> buildListDateTimeQuery(columnName, value, min, max, filterType, false)
            FilterMode.ListAll -> buildListDateTimeQuery(columnName, value, min, max, filterType, true)
        }
    }

    private fun buildSingleDateTimeQuery(
        columnName: String,
        value: T?,
        min: T?,
        max: T?,
        filterType: DateTimeFilterDataType
    ): Query? {
        val paramName = columnName
        val minParamName = "${columnName}_min"
        val maxParamName = "${columnName}_max"

        return when (filterType) {
            DateTimeFilterDataType.Equals -> value?.let { Query("$columnName = :$paramName", mapOf(paramName to it)) }
            DateTimeFilterDataType.NotEquals -> value?.let { Query("$columnName != :$paramName", mapOf(paramName to it)) }
            DateTimeFilterDataType.Before -> value?.let { Query("$columnName < :$paramName", mapOf(paramName to it)) }
            DateTimeFilterDataType.BeforeEquals -> value?.let { Query("$columnName <= :$paramName", mapOf(paramName to it)) }
            DateTimeFilterDataType.After -> value?.let { Query("$columnName > :$paramName", mapOf(paramName to it)) }
            DateTimeFilterDataType.AfterEquals -> value?.let { Query("$columnName >= :$paramName", mapOf(paramName to it)) }
            DateTimeFilterDataType.Range -> when {
                min != null && max != null -> Query(
                    "$columnName BETWEEN :$minParamName AND :$maxParamName",
                    mapOf(minParamName to min, maxParamName to max)
                )
                min != null -> Query("$columnName >= :$minParamName", mapOf(minParamName to min))
                max != null -> Query("$columnName <= :$maxParamName", mapOf(maxParamName to max))
                else -> null
            }
        }
    }

    private fun buildListDateTimeQuery(
        columnName: String,
        value: T?,
        min: T?,
        max: T?,
        filterType: DateTimeFilterDataType,
        isAllMode: Boolean
    ): Query? {
        val paramName = columnName
        val minParamName = "${columnName}_min"
        val maxParamName = "${columnName}_max"

        fun buildExistsQuery(condition: String, paramMap: Map<String, Any>): Query {
            return if (isAllMode) {
                Query("NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE NOT ($condition))", paramMap)
            } else {
                Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition)", paramMap)
            }
        }

        return when (filterType) {
            DateTimeFilterDataType.Equals -> value?.let {
                val operator = if (isAllMode) "@>" else "&&"
                Query("$columnName $operator :$paramName", mapOf(paramName to listOf(it)))
            }
            DateTimeFilterDataType.NotEquals -> value?.let {
                val operator = if (isAllMode) "@>" else "&&"
                Query("NOT ($columnName $operator :$paramName)", mapOf(paramName to listOf(it)))
            }
            DateTimeFilterDataType.Before -> value?.let {
                buildExistsQuery("elem < :$paramName", mapOf(paramName to it))
            }
            DateTimeFilterDataType.BeforeEquals -> value?.let {
                buildExistsQuery("elem <= :$paramName", mapOf(paramName to it))
            }
            DateTimeFilterDataType.After -> value?.let {
                buildExistsQuery("elem > :$paramName", mapOf(paramName to it))
            }
            DateTimeFilterDataType.AfterEquals -> value?.let {
                buildExistsQuery("elem >= :$paramName", mapOf(paramName to it))
            }
            DateTimeFilterDataType.Range -> when {
                min != null && max != null -> buildExistsQuery(
                    "elem BETWEEN :$minParamName AND :$maxParamName",
                    mapOf(minParamName to min, maxParamName to max)
                )
                min != null -> buildExistsQuery("elem >= :$minParamName", mapOf(minParamName to min))
                max != null -> buildExistsQuery("elem <= :$maxParamName", mapOf(maxParamName to max))
                else -> null
            }
        }
    }
}