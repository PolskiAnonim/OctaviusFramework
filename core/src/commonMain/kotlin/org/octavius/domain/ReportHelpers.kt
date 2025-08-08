package org.octavius.domain

import kotlinx.serialization.json.JsonObject

enum class SortDirection {
    Ascending, // Rosnąca
    Descending // Malejąca
}

data class SortConfiguration(
    val columnName: String,
    val sortDirection: SortDirection
)

data class FilterConfig(
    val columnName: String,
    val config: JsonObject
)