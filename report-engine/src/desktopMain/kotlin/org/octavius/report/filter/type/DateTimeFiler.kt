package org.octavius.report.filter.type

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import org.octavius.ui.datetime.DateTimePickerDialog
import org.octavius.ui.datetime.PickerTextField
import org.octavius.util.DateTimeAdapter
import kotlin.reflect.KClass

class DateTimeFilter<T : Any>(
    private val kClass: KClass<T>,
    private val adapter: DateTimeAdapter<T>
) : Filter<DateTimeFilterData<T>>() {

    override fun createDefaultData(): DateTimeFilterData<T> = DateTimeFilterData(kClass, adapter)

    override fun deserializeData(data: JsonObject): DateTimeFilterData<T> = DateTimeFilterData.deserialize(data, kClass, adapter)

    @Composable
    override fun RenderFilterUI(onEvent: (ReportEvent) -> Unit, columnKey: String, data: DateTimeFilterData<T>) {
        var showDialog by remember { mutableStateOf(false) }
        var isEditingMinOrValue by remember { mutableStateOf(true) }

        EnumDropdownMenu(
            currentValue = data.filterType,
            options = DateTimeFilterDataType.entries,
            onValueChange = { onEvent(ReportEvent.FilterChanged(columnKey, data.copy(filterType = it))) }
        )

        FilterSpacer()

        if (data.filterType == DateTimeFilterDataType.Range) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PickerTextField(
                    modifier = Modifier.weight(1f),
                    label = T.get("filter.datetime.from"),
                    value = adapter.format(data.minValue),
                    onClick = {
                        isEditingMinOrValue = true
                        showDialog = true
                    }
                )
                PickerTextField(
                    modifier = Modifier.weight(1f),
                    label = T.get("filter.datetime.to"),
                    value = adapter.format(data.maxValue),
                    onClick = {
                        isEditingMinOrValue = false
                        showDialog = true
                    }
                )
            }
        } else {
            PickerTextField(
                modifier = Modifier.fillMaxWidth(),
                label = T.get("filter.datetime.value"),
                value = adapter.format(data.value),
                onClick = {
                    isEditingMinOrValue = true
                    showDialog = true
                }
            )
        }

        if (showDialog) {
            val initialValueForDialog = if (isEditingMinOrValue) {
                if (data.filterType == DateTimeFilterDataType.Range) data.minValue else data.value
            } else {
                data.maxValue
            }

            DateTimePickerDialog(
                adapter = adapter,
                initialValue = initialValueForDialog,
                onDismiss = { showDialog = false },
                onConfirm = { newValue ->
                    showDialog = false
                    val updatedData = if (isEditingMinOrValue) {
                        if (data.filterType == DateTimeFilterDataType.Range) {
                            data.copy(minValue = newValue)
                        } else {
                            data.copy(value = newValue)
                        }
                    } else {
                        data.copy(maxValue = newValue)
                    }
                    onEvent(ReportEvent.FilterChanged(columnKey, updatedData))
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