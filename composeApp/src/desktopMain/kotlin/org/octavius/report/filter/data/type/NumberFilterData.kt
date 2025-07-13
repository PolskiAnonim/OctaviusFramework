package org.octavius.report.filter.data.type

import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.NumberFilterDataType
import org.octavius.report.filter.data.FilterData
import kotlin.reflect.KClass

data class NumberFilterData<T : Number>(
    val numberClass: KClass<T>,
    override val mode: FilterMode = FilterMode.Single,
    override val nullHandling: NullHandling = NullHandling.Ignore,
    val filterType: NumberFilterDataType = NumberFilterDataType.Equals,
    val minValue: T? = null,
    val maxValue: T? = null
) : FilterData {

    override fun isActive(): Boolean {
        return minValue != null || maxValue != null || nullHandling != NullHandling.Ignore
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("filterType", filterType.name)
            put("minValue", minValue)
            put("maxValue", maxValue)
            put("nullHandling", nullHandling.name)
            put("mode", mode.name)
        }
    }

    companion object {
        fun <T : Number> deserialize(data: JsonObject, numberClass: KClass<T>): NumberFilterData<T> {
            @Suppress("UNCHECKED_CAST")
            fun <T : Number> convertToNumber(jsonPrimitive: JsonPrimitive): T? {
                return when (numberClass) {
                    Int::class -> jsonPrimitive.intOrNull as T?
                    Long::class -> jsonPrimitive.longOrNull as T?
                    Float::class -> jsonPrimitive.floatOrNull as T?
                    Double::class -> jsonPrimitive.doubleOrNull as T?
                    else -> null
                }
            }

            return NumberFilterData(
                numberClass = numberClass,
                filterType = data["filterType"]!!.jsonPrimitive.content.let { NumberFilterDataType.valueOf(it) },
                minValue = data["minValue"]!!.jsonPrimitive.let { convertToNumber(it) },
                nullHandling = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) },
                maxValue = data["maxValue"]!!.jsonPrimitive.let { convertToNumber(it) },
                mode = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
            )
        }
    }

    override fun withMode(newMode: FilterMode): FilterData {
        return this.copy(mode = newMode)
    }

    override fun withNullHandling(newNullHandling: NullHandling): FilterData {
        return this.copy(nullHandling = newNullHandling)
    }
}