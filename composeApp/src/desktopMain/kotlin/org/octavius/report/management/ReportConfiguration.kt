package org.octavius.report.management

import org.octavius.domain.SortConfiguration

data class ReportConfiguration(
    val id: Int? = null,
    val name: String,
    val reportName: String,
    val description: String? = null,
    val configuration: ReportConfigurationData,
    val isDefault: Boolean = false
)


data class ReportConfigurationData(
    val visibleColumns: List<String>,
    val columnOrder: List<String>,
    val sortOrder: List<SortConfiguration>,
    val pageSize: Int = 10
)