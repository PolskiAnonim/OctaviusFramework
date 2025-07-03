package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.octavius.domain.FilterMode
import org.octavius.domain.NullHandling
import org.octavius.report.Query
import org.octavius.report.filter.data.FilterData

data class BooleanFilterData(
    val value: MutableState<Boolean?> = mutableStateOf(null),
    override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore),
    override val mode: MutableState<FilterMode> = mutableStateOf(FilterMode.Single)
) : FilterData() {

    override fun getFilterFragment(columnName: String): Query? {
        val boolValue = value.value
        if (boolValue == null && nullHandling.value == NullHandling.Ignore) return null
        
        val baseQuery = buildBooleanQuery(columnName, boolValue, mode.value)
        return applyNullHandling(baseQuery, columnName)
    }

    override fun resetValue() {
        value.value = null
    }

    private fun buildBooleanQuery(
        columnName: String,
        value: Boolean?,
        mode: FilterMode
    ): Query? {
        if (value == null) return null
        
        return when (mode) {
            FilterMode.Single -> {
                Query("$columnName = :$columnName", mapOf(columnName to value))
            }
            FilterMode.ListAny -> {
                Query("$columnName && :$columnName", mapOf(columnName to listOf(value)))
            }
            FilterMode.ListAll -> {
                Query("$columnName @> :$columnName", mapOf(columnName to listOf(value)))
            }
        }
    }
}