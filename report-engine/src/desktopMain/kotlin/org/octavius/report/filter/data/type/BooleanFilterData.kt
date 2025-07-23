package org.octavius.report.filter.data.type

import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.filter.data.FilterData

data class BooleanFilterData(
    override val mode: FilterMode = FilterMode.Single,
    override val nullHandling: NullHandling = NullHandling.Ignore,
    val value: Boolean? = null
) : FilterData {

    override fun isActive(): Boolean {
        return value != null || nullHandling != NullHandling.Ignore
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("value", value)
            put("nullHandling", nullHandling.name)
            put("mode", mode.name)
        }
    }

    companion object {
        fun deserialize(data: JsonObject): BooleanFilterData {
            return BooleanFilterData(
                value = data["value"]!!.jsonPrimitive.booleanOrNull,
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
