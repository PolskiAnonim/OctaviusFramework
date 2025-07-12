package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.data.FilterData

class StringFilterData: FilterData() {
    val filterType: MutableState<StringFilterDataType> = mutableStateOf(StringFilterDataType.Contains)
    val value: MutableState<String> = mutableStateOf("")
    val caseSensitive: MutableState<Boolean> = mutableStateOf(false)

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
        resetFilterData()
        filterType.value = data["filterType"]!!.jsonPrimitive.content.let { StringFilterDataType.valueOf(it) }
        value.value = data["value"]!!.jsonPrimitive.content
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }
}