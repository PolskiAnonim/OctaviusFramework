package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.json.*
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.filter.data.FilterData
import kotlin.reflect.KClass

class EnumFilterData<E : Enum<E>>(val enumClass: KClass<E>) : FilterData() {
    val values: SnapshotStateList<E> = mutableStateListOf()
    val include: MutableState<Boolean> = mutableStateOf(true)

    override fun resetValue() {
        values.clear()
    }

    override fun isActive(): Boolean {
        return values.isNotEmpty() || nullHandling.value != NullHandling.Ignore
    }

    override fun getTrackableStates(): List<Any?> {
        return listOf(values.toList(), include.value, nullHandling.value, mode.value)
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            putJsonArray("values") {
               for (value in values) {
                   add(value.name)
               }
            }
            put("include", include.value)
            put("nullHandling", nullHandling.value.name)
            put("mode", mode.value.name)
        }
    }

    override fun deserialize(data: JsonObject) {
        resetFilterData()
        values.addAll(data["values"]!!.jsonArray.map { element -> 
            enumClass.java.enumConstants.first { it.name == element.jsonPrimitive.content }
        })
        include.value = data["include"]!!.jsonPrimitive.boolean
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }
}