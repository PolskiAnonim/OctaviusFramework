package org.octavius.domain

import kotlinx.serialization.json.JsonObject
import org.octavius.data.PgType

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