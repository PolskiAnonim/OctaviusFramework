package org.octavius.report.filter.data.type

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.octavius.domain.FilterMode
import org.octavius.domain.NullHandling
import org.octavius.report.Query
import org.octavius.report.filter.data.FilterData

data class EnumFilterData<E : Enum<E>>(
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
}