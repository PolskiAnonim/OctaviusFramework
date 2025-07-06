package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.Query
import org.octavius.report.filter.data.FilterData

data class BooleanFilterData(
    val value: MutableState<Boolean?> = mutableStateOf(null),
    override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore),
    override val mode: MutableState<FilterMode> = mutableStateOf(FilterMode.Single)
) : FilterData() {

    override fun getFilterFragment(columnName: String): Query? {
        val boolValue = value.value
        if (!isActive()) return null

        val baseQuery = buildBooleanQuery(columnName, boolValue, mode.value)
        return applyNullHandling(baseQuery, columnName)
    }

    override fun resetValue() {
        value.value = null
    }

    override fun isActive(): Boolean {
        return value.value != null || nullHandling.value != NullHandling.Ignore
    }

    override fun getTrackableStates(): List<Any?> {
        return listOf(value.value, nullHandling.value, mode.value)
    }

    private fun buildBooleanQuery(
        columnName: String,
        value: Boolean?,
        mode: FilterMode
    ): Query? {
        if (value == null) return null

        return when (mode) {
            FilterMode.Single -> {
                Query("$columnName = :$columnName", mapOf(columnName to value))
            }

            FilterMode.ListAny -> {
                Query("$columnName && :$columnName", mapOf(columnName to listOf(value)))
            }

            FilterMode.ListAll -> {
                Query("$columnName @> :$columnName", mapOf(columnName to listOf(value)))
            }
        }
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("value", value.value)
            put("nullHandling", nullHandling.value.name)
            put("mode", mode.value.name)
        }
    }

    override fun deserialize(data: JsonObject) {
        resetFilter()
        value.value = data["value"]!!.jsonPrimitive.booleanOrNull
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }
}
