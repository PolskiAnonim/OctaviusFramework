package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.octavius.domain.FilterMode
import org.octavius.domain.NullHandling
import org.octavius.domain.NumberFilterDataType
import org.octavius.report.Query
import org.octavius.report.filter.data.FilterData
import kotlin.reflect.KClass

data class NumberFilterData<T : Number>(
    val numberClass: KClass<T>,
    val filterType: MutableState<NumberFilterDataType> = mutableStateOf(NumberFilterDataType.Equals),
    val minValue: MutableState<T?> = mutableStateOf(null),
    val maxValue: MutableState<T?> = mutableStateOf(null),
    override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore),
    override val mode: MutableState<FilterMode> = mutableStateOf(FilterMode.Single)
) : FilterData() {

    override fun resetValue() {
        filterType.value = NumberFilterDataType.Equals
        minValue.value = null
        maxValue.value = null
    }

    override fun isActive(): Boolean {
        return minValue.value != null || maxValue.value != null || nullHandling.value != NullHandling.Ignore
    }

    override fun getTrackableStates(): List<Any?> {
        return listOf(filterType.value, minValue.value, maxValue.value, nullHandling.value, mode.value)
    }

    override fun getFilterFragment(columnName: String): Query? {
        if (!isActive()) return null

        val min = minValue.value
        val max = maxValue.value
        
        val baseQuery = buildNumberQuery(columnName, min, max, mode.value, filterType.value)
        return applyNullHandling(baseQuery, columnName)
    }

    private fun buildNumberQuery(
        columnName: String,
        min: T?,
        max: T?,
        mode: FilterMode,
        filterType: NumberFilterDataType
    ): Query? {
        val hasValues = min != null || max != null
        if (!hasValues) return null
        
        return when (mode) {
            FilterMode.Single -> buildSingleNumberQuery(columnName, min, max, filterType)
            FilterMode.ListAny -> buildListNumberQuery(columnName, min, max, filterType, "&&")
            FilterMode.ListAll -> buildListNumberQuery(columnName, min, max, filterType, "@>")
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
        operator: String
    ): Query? {
        return when (filterType) {
            NumberFilterDataType.Equals -> {
                if (min != null) {
                    Query("$columnName $operator :$columnName", mapOf(columnName to listOf(min)))
                } else null
            }
            NumberFilterDataType.NotEquals -> {
                if (min != null) {
                    Query("NOT ($columnName $operator :$columnName)", mapOf(columnName to listOf(min)))
                } else null
            }
            NumberFilterDataType.LessThan -> {
                if (max != null) {
                    Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem < :$columnName)", mapOf(columnName to max))
                } else null
            }
            NumberFilterDataType.LessEquals -> {
                if (max != null) {
                    Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)", mapOf(columnName to max))
                } else null
            }
            NumberFilterDataType.GreaterThan -> {
                if (min != null) {
                    Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem > :$columnName)", mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.GreaterEquals -> {
                if (min != null) {
                    Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)", mapOf(columnName to min))
                } else null
            }
            NumberFilterDataType.Range -> {
                when {
                    min != null && max != null -> {
                        Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem BETWEEN :${columnName}_min AND :${columnName}_max)",
                            mapOf("${columnName}_min" to min, "${columnName}_max" to max))
                    }
                    min != null -> {
                        Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem >= :$columnName)", mapOf(columnName to min))
                    }
                    max != null -> {
                        Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE elem <= :$columnName)", mapOf(columnName to max))
                    }
                    else -> null
                }
            }
        }
    }
}