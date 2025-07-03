package org.octavius.report.filter.data

import androidx.compose.runtime.MutableState
import org.octavius.domain.EnumWithFormatter
import org.octavius.domain.FilterMode
import org.octavius.domain.NullHandling
import org.octavius.report.Query

abstract class FilterData {
    abstract val nullHandling: MutableState<NullHandling>
    abstract val mode: MutableState<FilterMode>

    abstract fun getFilterFragment(columnName: String): Query?

    fun resetFilter() {
        nullHandling.value = NullHandling.Ignore
        mode.value = if (mode.value == FilterMode.ListAll) FilterMode.ListAny else mode.value
        resetValue()
    }

    protected abstract fun resetValue()

    abstract fun isActive(): Boolean

    protected fun applyNullHandling(baseQuery: Query?, columnName: String): Query? {
        return when (nullHandling.value) {
            NullHandling.Ignore -> baseQuery
            NullHandling.Include -> baseQuery?.let {
                Query("(${it.sql} OR $columnName IS NULL)", it.params)
            } ?: Query("$columnName IS NULL")
            NullHandling.Exclude -> baseQuery?.let {
                Query("(${it.sql} AND $columnName IS NOT NULL)", it.params)
            } ?: Query("$columnName IS NOT NULL")
        }
    }
}