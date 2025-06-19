package org.octavius.report


enum class SortDirection {
    Ascending, // Rosnąca
    Descending // Malejąca
}

data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)