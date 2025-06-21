package org.octavius.report

import kotlinx.serialization.Serializable
import org.octavius.domain.SortDirection

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
    val filterValues: Map<String, SerializableFilterData>,
    val pageSize: Int = 10
)

@Serializable
data class SortConfiguration(
    val columnName: String,
    val direction: SortDirection
)

@Serializable
sealed class SerializableFilterData {
    @Serializable
    data class BooleanFilter(
        val value: Boolean?,
        val nullHandling: String
    ) : SerializableFilterData()

    @Serializable
    data class NumberFilter(
        val filterType: String,
        val minValue: Double?,
        val maxValue: Double?, 
        val nullHandling: String
    ) : SerializableFilterData()

    @Serializable
    data class StringFilter(
        val filterType: String,
        val value: String,
        val caseSensitive: Boolean,
        val nullHandling: String
    ) : SerializableFilterData()

    @Serializable
    data class EnumFilter(
        val values: List<String>,
        val include: Boolean,
        val nullHandling: String
    ) : SerializableFilterData()
}