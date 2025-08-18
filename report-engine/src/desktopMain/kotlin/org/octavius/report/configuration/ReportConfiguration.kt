package org.octavius.report.configuration

import org.octavius.domain.FilterConfig
import org.octavius.domain.SortConfiguration
import org.octavius.util.MapKey

data class ReportConfiguration(
    val id: Int? = null,
    val name: String,
    @MapKey("report_name") val reportName: String,
    val description: String? = null,
    @MapKey("is_default") val isDefault: Boolean = false,
    @MapKey("visible_columns") val visibleColumns: List<String>,
    @MapKey("column_order") val columnOrder: List<String>,
    @MapKey("sort_order") val sortOrder: List<SortConfiguration>,
    @MapKey("page_size") val pageSize: Int,
    val filters: List<FilterConfig>
)