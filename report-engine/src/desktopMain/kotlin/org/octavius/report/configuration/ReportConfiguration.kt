package org.octavius.report.configuration

import kotlinx.serialization.json.JsonObject
import org.octavius.data.annotation.PgComposite
import org.octavius.data.annotation.PgEnum

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

@PgEnum
enum class SortDirection {
    Ascending, // Rosnąca
    Descending // Malejąca
}

@PgComposite
data class SortConfiguration(
    val columnName: String,
    val sortDirection: SortDirection
)

@PgComposite
data class FilterConfig(
    val columnName: String,
    val config: JsonObject
)