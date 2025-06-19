package org.octavius.report

import kotlinx.serialization.Serializable

@Serializable
data class ReportConfiguration(
    val id: Int? = null,
    val name: String,
    val reportName: String,
    val description: String? = null,
    val configuration: ReportConfigurationData,
    val isDefault: Boolean = false
)

@Serializable
data class ReportConfigurationData(
    val visibleColumns: List<String>,
    val columnOrder: List<String>,
    val sortOrder: List<SortConfiguration>,
    val filterValues: Map<String, String>, // Serialized filter data
    val pageSize: Int = 10
)

@Serializable
data class SortConfiguration(
    val columnName: String,
    val direction: SortDirection
)