package org.octavius.report.filter.type

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.FilterMode
import org.octavius.report.NumberFilterDataType
import org.octavius.report.Query
import org.octavius.report.filter.EnumDropdownMenu
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.NumberFilterData
import kotlin.reflect.KClass

class NumberFilter<T : Number>(
    private val numberClass: KClass<T>,
    private val valueParser: (String) -> T?
) : Filter<NumberFilterData<T>>() {
    override fun createDefaultData(): NumberFilterData<T> {
        return NumberFilterData(numberClass)
    }

    @Composable
    override fun RenderFilterUI(data: NumberFilterData<T>) {
        EnumDropdownMenu(
            currentValue = data.filterType.value,
            options = NumberFilterDataType.entries,
            onValueChange = { data.filterType.value = it }
        )

        FilterSpacer()

        if (data.filterType.value == NumberFilterDataType.Range) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = data.minValue.value?.toString() ?: "",
                    onValueChange = { input ->
                        try {
                            data.minValue.value = if (input.isEmpty()) null else valueParser(input)
                        } catch (e: NumberFormatException) {
                            // Ignore invalid input
                            data.minValue.value = null
                        }
                    },
                    label = { Text(Translations.get("filter.number.from")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = data.maxValue.value?.toString() ?: "",
                    onValueChange = { input ->
                        try {
                            data.maxValue.value = if (input.isEmpty()) null else valueParser(input)
                        } catch (e: NumberFormatException) {
                            // Ignore invalid input
                            data.maxValue.value = null
                        }
                    },
                    label = { Text(Translations.get("filter.number.to")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = data.minValue.value?.toString() ?: "",
                onValueChange = { input ->
                    try {
                        data.minValue.value = if (input.isEmpty()) null else valueParser(input)
                    } catch (e: NumberFormatException) {
                        // Ignore invalid input
                        data.minValue.value = null
                    }
                },
                label = { Text(Translations.get("filter.number.value")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (data.minValue.value != null) {
                        IconButton(onClick = { data.minValue.value = null }) {
                            Icon(Icons.Default.Clear, Translations.get("filter.general.clear"))
                        }
                    }
                }
            )
        }
    }

    override fun buildBaseQueryFragment(
        columnName: String,
        data: NumberFilterData<T>
    ): Query? {
        val min = data.minValue.value
        val max = data.maxValue.value
        val filterType = data.filterType.value

        return when (data.mode.value) {
            FilterMode.Single -> buildSingleNumberQuery(columnName, min, max, filterType)
            FilterMode.ListAny -> buildListNumberQuery(columnName, min, max, filterType, false)
            FilterMode.ListAll -> buildListNumberQuery(columnName, min, max, filterType, true)
        }
    }

    private fun buildSingleNumberQuery(
        columnName: String,
        min: T?,
        max: T?,
        filterType: NumberFilterDataType
    ): Query? {
        return when (filterType) {
            NumberFilterDataType.Equals -> {
                if (min != null) {
                    Query("$columnName = :$columnName", mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.NotEquals -> {
                if (min != null) {
                    Query("$columnName != :$columnName", mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.LessThan -> {
                if (max != null) {
                    Query("$columnName < :$columnName", mapOf(columnName to max))
                } else null
            }
            NumberFilterDataType.LessEquals -> {
                if (max != null) {
                    Query("$columnName <= :$columnName", mapOf(columnName to max))
                } else null
            }
            NumberFilterDataType.GreaterThan -> {
                if (min != null) {
                    Query("$columnName > :$columnName", mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.GreaterEquals -> {
                if (min != null) {
                    Query("$columnName >= :$columnName", mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.Range -> {
                when {
                    min != null && max != null -> {
                        Query("$columnName BETWEEN :${columnName}_min AND :${columnName}_max",
                            mapOf("${columnName}_min" to min, "${columnName}_max" to max))
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

    private fun buildListNumberQuery(
        columnName: String,
        min: T?,
        max: T?,
        filterType: NumberFilterDataType,
        isAllMode: Boolean
    ): Query? {
        return when (filterType) {
            NumberFilterDataType.Equals -> {
                if (min != null) {
                    val operator = if (isAllMode) "@>" else "&&"
                    Query("$columnName $operator :$columnName", mapOf(columnName to listOf(min)))
                } else null
            }
            NumberFilterDataType.NotEquals -> {
                if (min != null) {
                    val operator = if (isAllMode) "@>" else "&&"
                    Query("NOT ($columnName $operator :$columnName)", mapOf(columnName to listOf(min)))
                } else null
            }
            NumberFilterDataType.LessThan -> {
                if (max != null) {
                    val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)"
                    Query(existsType, mapOf(columnName to max))
                } else null
            }
            NumberFilterDataType.LessEquals -> {
                if (max != null) {
                    val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)"
                    Query(existsType, mapOf(columnName to max))
                } else null
            }
            NumberFilterDataType.GreaterThan -> {
                if (min != null) {
                    val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)"
                    Query(existsType, mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.GreaterEquals -> {
                if (min != null) {
                    val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)"
                    Query(existsType, mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.Range -> {
                when {
                    min != null && max != null -> {
                        val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem NOT BETWEEN :${columnName}_min AND :${columnName}_max)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem BETWEEN :${columnName}_min AND :${columnName}_max)"
                        Query(existsType, mapOf("${columnName}_min" to min, "${columnName}_max" to max))
                    }
                    min != null -> {
                        val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)"
                        Query(existsType, mapOf(columnName to min))
                    }
                    max != null -> {
                        val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)"
                        Query(existsType, mapOf(columnName to max))
                    }
                    else -> null
                }
            }
        }
    }
}