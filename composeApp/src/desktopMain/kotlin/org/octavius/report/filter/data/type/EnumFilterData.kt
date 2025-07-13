package org.octavius.report.filter.data.type

import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.filter.data.FilterData
import kotlin.reflect.KClass

data class EnumFilterData<E : Enum<E>>(
    val enumClass: KClass<E>,
    override val mode: FilterMode = FilterMode.Single,
    override val nullHandling: NullHandling = NullHandling.Ignore,
    val values: List<E> = emptyList(),
    val include: Boolean = true
) : FilterData {

    override fun isActive(): Boolean {
        return values.isNotEmpty() || nullHandling != NullHandling.Ignore
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            putJsonArray("values") {
                for (value in values) {
                    add(value.name)
                }
            }
            put("include", include)
            put("nullHandling", nullHandling.name)
            put("mode", mode.name)
        }
    }


    companion object {
        fun <E: Enum<E>> deserialize(data: JsonObject, enumClass: KClass<E>): EnumFilterData<E> {
            return EnumFilterData(
                enumClass = enumClass,
                values = data["values"]!!.jsonArray.map { element ->
                    enumClass.java.enumConstants.first { it.name == element.jsonPrimitive.content }
                },
                include = data["include"]!!.jsonPrimitive.boolean,
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