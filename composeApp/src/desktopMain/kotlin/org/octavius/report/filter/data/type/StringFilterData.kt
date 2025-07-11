package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.Query
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.data.FilterData

data class StringFilterData(
    val filterType: MutableState<StringFilterDataType> = mutableStateOf(StringFilterDataType.Contains),
    val value: MutableState<String> = mutableStateOf(""),
    val caseSensitive: MutableState<Boolean> = mutableStateOf(false),
    override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore),
    override val mode: MutableState<FilterMode> = mutableStateOf(FilterMode.Single)
) : FilterData() {

    override fun resetValue() {
        filterType.value = StringFilterDataType.Contains
        value.value = ""
        caseSensitive.value = false
    }

    override fun isActive(): Boolean {
        return value.value.trim().isNotEmpty() || nullHandling.value != NullHandling.Ignore
    }

    override fun getTrackableStates(): List<Any?> {
        return listOf(filterType.value, value.value, caseSensitive.value, nullHandling.value, mode.value)
    }

    override fun getFilterFragment(columnName: String): Query? {
        if (!isActive()) return null
        val searchValue = value.value.trim()
        
        val baseQuery = buildStringQuery(columnName, searchValue, mode.value, filterType.value, caseSensitive.value)
        return applyNullHandling(baseQuery, columnName)
    }
    
    private fun buildStringQuery(
        columnName: String,
        searchValue: String,
        mode: FilterMode,
        filterType: StringFilterDataType,
        caseSensitive: Boolean
    ): Query? {
        if (searchValue.isEmpty()) return null
        
        return when (mode) {
            FilterMode.Single -> buildSingleStringQuery(columnName, searchValue, filterType, caseSensitive)
            FilterMode.ListAny -> buildListStringQuery(columnName, searchValue, filterType, caseSensitive, false)
            FilterMode.ListAll -> buildListStringQuery(columnName, searchValue, filterType, caseSensitive, true)
        }
    }
    
    private fun buildSingleStringQuery(
        columnName: String,
        searchValue: String,
        filterType: StringFilterDataType,
        caseSensitive: Boolean
    ): Query {
        val columnRef = if (caseSensitive) columnName else "LOWER($columnName)"
        val valueRef = if (caseSensitive) searchValue else searchValue.lowercase()
        
        return when (filterType) {
            StringFilterDataType.Exact -> {
                Query("$columnRef = :$columnName", mapOf(columnName to valueRef))
            }
            StringFilterDataType.StartsWith -> {
                Query("$columnRef LIKE :$columnName", mapOf(columnName to "$valueRef%"))
            }
            StringFilterDataType.EndsWith -> {
                Query("$columnRef LIKE :$columnName", mapOf(columnName to "%$valueRef"))
            }
            StringFilterDataType.Contains -> {
                Query("$columnRef LIKE :$columnName", mapOf(columnName to "%$valueRef%"))
            }
            StringFilterDataType.NotContains -> {
                Query("$columnRef NOT LIKE :$columnName", mapOf(columnName to "%$valueRef%"))
            }
        }
    }
    
    private fun buildListStringQuery(
        columnName: String,
        searchValue: String,
        filterType: StringFilterDataType,
        caseSensitive: Boolean,
        isAllMode: Boolean
    ): Query {
        val valueParam = if (caseSensitive) searchValue else searchValue.lowercase()
        
        return when (filterType) {
            StringFilterDataType.Exact -> {
                val operator = if (isAllMode) "@>" else "&&"
                Query("$columnName $operator :$columnName", mapOf(columnName to listOf(valueParam)))
            }
            StringFilterDataType.StartsWith -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition NOT LIKE :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "$valueParam%"))
            }
            StringFilterDataType.EndsWith -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition NOT LIKE :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "%$valueParam"))
            }
            StringFilterDataType.Contains -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition NOT LIKE :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "%$valueParam%"))
            }
            StringFilterDataType.NotContains -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)" else "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "%$valueParam%"))
            }
        }
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("filterType", filterType.value.name)
            put("value", value.value)
            put("caseSensitive", caseSensitive.value)
            put("nullHandling", nullHandling.value.name)
            put("mode", mode.value.name)
        }
    }

    override fun deserialize(data: JsonObject) {
        resetFilter()
        filterType.value = data["filterType"]!!.jsonPrimitive.content.let { StringFilterDataType.valueOf(it) }
        value.value = data["value"]!!.jsonPrimitive.content
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }
}