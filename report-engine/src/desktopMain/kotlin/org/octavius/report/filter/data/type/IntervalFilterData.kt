package org.octavius.report.filter.data.type

import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.IntervalFilterDataType
import org.octavius.report.NullHandling
import org.octavius.report.filter.data.FilterData
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class IntervalFilterData(
    override val mode: FilterMode = FilterMode.Single,
    override val nullHandling: NullHandling = NullHandling.Ignore,
    val filterType: IntervalFilterDataType = IntervalFilterDataType.Equals,
    val value: Duration? = null,
    val minValue: Duration? = null,
    val maxValue: Duration? = null
) : FilterData {

    override fun isActive(): Boolean {
        return value != null || minValue != null || maxValue != null || nullHandling != NullHandling.Ignore
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("filterType", filterType.name)
            put("value", value?.toString())
            put("minValue", minValue?.toString())
            put("maxValue", maxValue?.toString())
            put("nullHandling", nullHandling.name)
            put("mode", mode.name)
        }
    }

    companion object {
        fun deserialize(data: JsonObject): IntervalFilterData {
            fun parseDurationOrNull(jsonPrimitive: JsonPrimitive?): Duration? {
                return jsonPrimitive?.contentOrNull?.let {
                    try {
                        Duration.parse(it)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }

            return IntervalFilterData(
                filterType = data["filterType"]!!.jsonPrimitive.content.let { IntervalFilterDataType.valueOf(it) },
                value = parseDurationOrNull(data["value"]?.jsonPrimitive),
                minValue = parseDurationOrNull(data["minValue"]?.jsonPrimitive),
                maxValue = parseDurationOrNull(data["maxValue"]?.jsonPrimitive),
                nullHandling = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) },
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