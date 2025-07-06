package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.NumberFilterDataType
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

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("filterType", filterType.value.name)
            put("minValue", minValue.value)
            put("maxValue", maxValue.value)
            put("nullHandling", nullHandling.value.name)
            put("mode", mode.value.name)
        }
    }

    override fun deserialize(data: JsonObject) {
        resetFilter()
        filterType.value = data["filterType"]!!.jsonPrimitive.content.let { NumberFilterDataType.valueOf(it) }
        minValue.value = data["minValue"]!!.jsonPrimitive.let { convertToNumber(it) }
        maxValue.value = data["maxValue"]!!.jsonPrimitive.let { convertToNumber(it) }
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToNumber(jsonPrimitive: JsonPrimitive): T? {
        return when (numberClass) {
            Int::class -> jsonPrimitive.intOrNull as T?
            Long::class -> jsonPrimitive.longOrNull as T?
            Float::class -> jsonPrimitive.floatOrNull as T?
            Double::class -> jsonPrimitive.doubleOrNull as T?
            else -> null
        }
    }
}