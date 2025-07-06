package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.Query
import org.octavius.report.filter.data.FilterData
import kotlin.reflect.KClass

data class EnumFilterData<E : Enum<E>>(
    val enumClass: KClass<E>,
    val values: SnapshotStateList<E> = mutableStateListOf(),
    val include: MutableState<Boolean> = mutableStateOf(true),
    override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore),
    override val mode: MutableState<FilterMode> = mutableStateOf(FilterMode.Single)
) : FilterData() {

    override fun resetValue() {
        values.clear()
    }

    override fun isActive(): Boolean {
        return values.isNotEmpty() || nullHandling.value != NullHandling.Ignore
    }

    override fun getTrackableStates(): List<Any?> {
        return listOf(values.toList(), include.value, nullHandling.value, mode.value)
    }

    override fun getFilterFragment(columnName: String): Query? {
        if (!isActive()) return null
        
        val selectedValues = values
        
        val baseQuery = buildEnumQuery(columnName, selectedValues, mode.value, include.value)
        return applyNullHandling(baseQuery, columnName)
    }
    
    private fun buildEnumQuery(
        columnName: String,
        values: List<E>,
        mode: FilterMode,
        include: Boolean
    ): Query? {
        if (values.isEmpty()) return null
        
        return when (mode) {
            FilterMode.Single -> {
                if (include) {
                    Query("$columnName = ANY(:$columnName)", mapOf(columnName to values))
                } else {
                    Query("$columnName != ALL(:$columnName)", mapOf(columnName to values))
                }
            }
            FilterMode.ListAny -> {
                if (include) {
                    Query("$columnName && :$columnName", mapOf(columnName to values))
                } else {
                    Query("NOT ($columnName && :$columnName)", mapOf(columnName to values))
                }
            }
            FilterMode.ListAll -> {
                if (include) {
                    Query("$columnName @> :$columnName", mapOf(columnName to values))
                } else {
                    Query("NOT ($columnName @> :$columnName)", mapOf(columnName to values))
                }
            }
        }
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
        resetFilter()
        values.addAll(data["values"]!!.jsonArray.map { element -> 
            enumClass.java.enumConstants.first { it.name == element.jsonPrimitive.content }
        })
        include.value = data["include"]!!.jsonPrimitive.boolean
        nullHandling.value = data["nullHandling"]!!.jsonPrimitive.content.let { NullHandling.valueOf(it) }
        mode.value = data["mode"]!!.jsonPrimitive.content.let { FilterMode.valueOf(it) }
    }
}