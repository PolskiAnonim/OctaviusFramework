package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.NumberFilterDataType
import org.octavius.report.filter.data.FilterData
import kotlin.reflect.KClass

class NumberFilterData<T : Number>(val numberClass: KClass<T>) : FilterData() {
    val filterType: MutableState<NumberFilterDataType> = mutableStateOf(NumberFilterDataType.Equals)
    val minValue: MutableState<T?> = mutableStateOf(null)
    val maxValue: MutableState<T?> = mutableStateOf(null)

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
        resetFilterData()
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