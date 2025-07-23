package org.octavius.report.filter.data.type

import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.data.FilterData

data class StringFilterData(
    override val mode: FilterMode = FilterMode.Single,
    override val nullHandling: NullHandling = NullHandling.Ignore,
    val filterType: StringFilterDataType = StringFilterDataType.Contains,
    val value: String = "",
    val caseSensitive: Boolean = false
) : FilterData {


    override fun isActive(): Boolean {
        return value.trim().isNotEmpty() || nullHandling != NullHandling.Ignore
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("filterType", filterType.name)
            put("value", value)
            put("caseSensitive", caseSensitive)
            put("nullHandling", nullHandling.name)
            put("mode", mode.name)
        }
    }

    companion object {
        fun deserialize(data: JsonObject): StringFilterData {
            return StringFilterData(
                filterType = data["filterType"]!!.jsonPrimitive.content.let { StringFilterDataType.valueOf(it) },
                value = data["value"]!!.jsonPrimitive.content,
                caseSensitive = data["caseSensitive"]!!.jsonPrimitive.boolean,
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