package org.octavius.report


data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)