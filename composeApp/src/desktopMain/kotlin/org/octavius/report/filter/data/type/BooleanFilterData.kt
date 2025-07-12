package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.filter.data.FilterData

class BooleanFilterData: FilterData() {
    val value: MutableState<Boolean?> = mutableStateOf(null)

    override fun resetValue() {
        value.value = null
    }

    override fun isActive(): Boolean {
        return value.value != null || nullHandling.value != NullHandling.Ignore
    }

    override fun getTrackableStates(): List<Any?> {
        return listOf(value.value, nullHandling.value, mode.value)
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("value", value.value)
            put("nullHandling", nullHandling.value.name)
            put("mode", mode.value.name)
        }
    }

    override fun deserialize(data: JsonObject) {
        resetFilterData()
        value.value = data["value"]!!.jsonPrimitive.booleanOrNull
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }
}
