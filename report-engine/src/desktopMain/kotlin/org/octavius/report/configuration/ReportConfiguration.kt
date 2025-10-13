package org.octavius.report.configuration

import kotlinx.serialization.json.JsonObject
import org.octavius.data.annotation.PgType

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

@PgType
enum class SortDirection {
    Ascending, // Rosnąca
    Descending // Malejąca
}

@PgType
data class SortConfiguration(
    val columnName: String,
    val sortDirection: SortDirection
)

@PgType
data class FilterConfig(
    val columnName: String,
    val config: JsonObject
)