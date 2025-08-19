package org.octavius.report.configuration

import org.octavius.domain.FilterConfig
import org.octavius.domain.SortConfiguration

data class ReportConfiguration(
    val id: Int? = null,
    val name: String,
   val reportName: String,
    val description: String? = null,
    val isDefault: Boolean = false,
   val visibleColumns: List<String>,
    val columnOrder: List<String>,
    val sortOrder: List<SortConfiguration>,
    val pageSize: Long,
    val filters: List<FilterConfig>
)