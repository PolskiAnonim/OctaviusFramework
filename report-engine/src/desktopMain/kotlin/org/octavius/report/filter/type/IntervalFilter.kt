package org.octavius.report.filter.type

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import org.octavius.report.FilterMode
import org.octavius.report.IntervalFilterDataType
import org.octavius.report.Query
import org.octavius.report.ReportEvent
import org.octavius.report.filter.EnumDropdownMenu
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.IntervalFilterData
import org.octavius.ui.datetime.IntervalPickerDialog
import org.octavius.ui.datetime.PickerTextField
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class IntervalFilter : Filter<IntervalFilterData>() {

    /**
     * Formatuje Duration na czytelny string HH:MM:SS do wyświetlenia w UI.
     * Ignoruje części składowe większe niż godziny (dni, miesiące, lata).
     */
    private fun formatDurationForDisplay(duration: Duration?): String {
        return duration?.let {
            val totalSeconds = it.inWholeSeconds
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } ?: ""
    }

    override fun createDefaultData(): IntervalFilterData = IntervalFilterData()

    override fun deserializeData(data: JsonObject): IntervalFilterData = IntervalFilterData.deserialize(data)

    @Composable
    override fun RenderFilterUI(onEvent: (ReportEvent) -> Unit, columnKey: String, data: IntervalFilterData) {
        // Stany do zarządzania widocznością dialogów dla każdego pola
        var showSingleValuePicker by remember { mutableStateOf(false) }
        var showMinValuePicker by remember { mutableStateOf(false) }
        var showMaxValuePicker by remember { mutableStateOf(false) }

        EnumDropdownMenu(
            currentValue = data.filterType,
            options = IntervalFilterDataType.entries,
            onValueChange = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(filterType = it))) }
        )

        FilterSpacer()

        if (data.filterType == IntervalFilterDataType.Range) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Picker dla minValue
                PickerTextField(
                    value = formatDurationForDisplay(data.minValue),
                    onClick = { showMinValuePicker = true },
                    onClear = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(minValue = null))) },
                    isRequired = false // Zakładamy, że minValue nie jest wymagane
                )
                // Picker dla maxValue
                PickerTextField(
                    value = formatDurationForDisplay(data.maxValue),
                    onClick = { showMaxValuePicker = true },
                    onClear = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(maxValue = null))) },
                    isRequired = false // Zakładamy, że maxValue nie jest wymagane
                )
            }
        } else {
            // Picker dla pojedynczej wartości
            PickerTextField(
                value = formatDurationForDisplay(data.value),
                onClick = { showSingleValuePicker = true },
                onClear = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(value = null))) },
                isRequired = false // Zakładamy, że wartość nie jest wymagana
            )
        }

        // --- Dialogi ---
        if (showSingleValuePicker) {
            IntervalPickerDialog(
                initialValue = data.value,
                onDismiss = { showSingleValuePicker = false },
                onConfirm = { newDuration ->
                    onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(value = newDuration)))
                    showSingleValuePicker = false
                }
            )
        }
        if (showMinValuePicker) {
            IntervalPickerDialog(
                initialValue = data.minValue,
                onDismiss = { showMinValuePicker = false },
                onConfirm = { newDuration ->
                    onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(minValue = newDuration)))
                    showMinValuePicker = false
                }
            )
        }
        if (showMaxValuePicker) {
            IntervalPickerDialog(
                initialValue = data.maxValue,
                onDismiss = { showMaxValuePicker = false },
                onConfirm = { newDuration ->
                    onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(maxValue = newDuration)))
                    showMaxValuePicker = false
                }
            )
        }
    }

    override fun buildBaseQueryFragment(
        columnName: String,
        data: IntervalFilterData
    ): Query? {
        val singleValue = data.value
        val min = data.minValue
        val max = data.maxValue
        val filterType = data.filterType

        return when (data.mode) {
            FilterMode.Single -> buildSingleIntervalQuery(columnName, singleValue, min, max, filterType)
            FilterMode.ListAny -> buildListIntervalQuery(columnName, singleValue, min, max, filterType, false)
            FilterMode.ListAll -> buildListIntervalQuery(columnName, singleValue, min, max, filterType, true)
        }
    }

    private fun buildSingleIntervalQuery(
        columnName: String,
        singleValue: Duration?,
        min: Duration?,
        max: Duration?,
        filterType: IntervalFilterDataType
    ): Query? {
        // Parametry typu interval w PostgreSQL są obsługiwane bezpośrednio
        return when (filterType) {
            IntervalFilterDataType.Equals -> {
                if (singleValue != null) {
                    Query("$columnName = :$columnName", mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.NotEquals -> {
                if (singleValue != null) {
                    Query("$columnName != :$columnName", mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.LessThan -> {
                if (singleValue != null) {
                    Query("$columnName < :$columnName", mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.LessEquals -> {
                if (singleValue != null) {
                    Query("$columnName <= :$columnName", mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.GreaterThan -> {
                if (singleValue != null) {
                    Query("$columnName > :$columnName", mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.GreaterEquals -> {
                if (singleValue != null) {
                    Query("$columnName >= :$columnName", mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.Range -> {
                when {
                    min != null && max != null -> {
                        Query(
                            "$columnName BETWEEN :${columnName}_min AND :${columnName}_max",
                            mapOf("${columnName}_min" to min, "${columnName}_max" to max)
                        )
                    }
                    min != null -> {
                        Query("$columnName >= :$columnName", mapOf(columnName to min))
                    }
                    max != null -> {
                        Query("$columnName <= :$columnName", mapOf(columnName to max))
                    }
                    else -> null
                }
            }
        }
    }

    private fun buildListIntervalQuery(
        columnName: String,
        singleValue: Duration?,
        min: Duration?,
        max: Duration?,
        filterType: IntervalFilterDataType,
        isAllMode: Boolean
    ): Query? {
        // Dla trybów ListAny/ListAll, używamy konstrukcji z unnest i EXISTS/NOT EXISTS,
        // podobnie jak w NumberFilter, ponieważ operatory `@>` i `&&` działają
        // tylko dla *równości* tablic, a nie dla porównań zakresowych.

        return when (filterType) {
            IntervalFilterDataType.Equals -> {
                if (singleValue != null) {
                    val operator = if (isAllMode) "@>" else "&&" // Dla równości tablic można użyć operatorów array
                    Query("$columnName $operator :$columnName", mapOf(columnName to listOf(singleValue)))
                } else null
            }
            IntervalFilterDataType.NotEquals -> {
                if (singleValue != null) {
                    val operator = if (isAllMode) "@>" else "&&"
                    Query("NOT ($columnName $operator :$columnName)", mapOf(columnName to listOf(singleValue)))
                } else null
            }
            IntervalFilterDataType.LessThan -> {
                if (singleValue != null) {
                    val existsType =
                        if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)"
                    Query(existsType, mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.LessEquals -> {
                if (singleValue != null) {
                    val existsType =
                        if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)"
                    Query(existsType, mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.GreaterThan -> {
                if (singleValue != null) {
                    val existsType =
                        if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)"
                    Query(existsType, mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.GreaterEquals -> {
                if (singleValue != null) {
                    val existsType =
                        if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)"
                    Query(existsType, mapOf(columnName to singleValue))
                } else null
            }
            IntervalFilterDataType.Range -> {
                when {
                    min != null && max != null -> {
                        val existsType =
                            if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem NOT BETWEEN :${columnName}_min AND :${columnName}_max)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem BETWEEN :${columnName}_min AND :${columnName}_max)"
                        Query(existsType, mapOf("${columnName}_min" to min, "${columnName}_max" to max))
                    }
                    min != null -> {
                        val existsType =
                            if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)"
                        Query(existsType, mapOf(columnName to min))
                    }
                    max != null -> {
                        val existsType =
                            if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)"
                        Query(existsType, mapOf(columnName to max))
                    }
                    else -> null
                }
            }
        }
    }
}