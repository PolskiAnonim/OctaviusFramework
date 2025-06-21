package org.octavius.report

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.octavius.database.DatabaseManager
import org.octavius.report.component.ReportState
import org.octavius.report.filter.Filter
import org.octavius.report.filter.type.EnumFilter
import androidx.compose.runtime.mutableStateOf

class ReportConfigurationManager {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    fun saveConfiguration(
        name: String,
        reportName: String,
        description: String?,
        reportState: ReportState,
        isDefault: Boolean = false
    ): Boolean {
        return try {
            val configData = ReportConfigurationData(
                visibleColumns = reportState.visibleColumns.value.toList(),
                columnOrder = reportState.columnKeys.toList(),
                sortOrder = reportState.sortOrder.value.map { (columnName, direction) ->
                    SortConfiguration(columnName, direction)
                },
                filterValues = serializeFilterValues(reportState.filterValues.value),
                pageSize = reportState.pageSize.value
            )
            
            val configJson = json.encodeToString(configData)
            
            val updater = DatabaseManager.getUpdater()
            val sql = """
                INSERT INTO public.report_configurations (name, report_name, description, configuration, is_default)
                VALUES (:name, :report_name, :description, :configuration::jsonb, :is_default)
                ON CONFLICT (name, report_name) 
                DO UPDATE SET 
                    description = EXCLUDED.description,
                    configuration = EXCLUDED.configuration,
                    is_default = EXCLUDED.is_default,
                    updated_at = CURRENT_TIMESTAMP
            """.trimIndent()
            
            val params = mapOf(
                "name" to name,
                "report_name" to reportName,
                "description" to description,
                "configuration" to configJson,
                "is_default" to isDefault
            )
            
            updater.executeUpdate(sql, params)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun loadConfiguration(name: String, reportName: String): ReportConfiguration? {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val params = mapOf(
                "name" to name,
                "report_name" to reportName
            )
            
            val result = fetcher.fetchRowOrNull(
                table = "public.report_configurations",
                fields = "id, name, report_name, description, configuration, is_default",
                filter = "name = :name AND report_name = :report_name",
                params = params
            )
            
            result?.let { row ->
                parseConfigurationFromRow(row)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val params = mapOf("report_name" to reportName)
            
            val result = fetcher.fetchRowOrNull(
                table = "public.report_configurations",
                fields = "id, name, report_name, description, configuration, is_default",
                filter = "report_name = :report_name AND is_default = true",
                params = params
            )
            
            result?.let { row ->
                parseConfigurationFromRow(row)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun listConfigurations(reportName: String): List<ReportConfiguration> {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val params = mapOf("report_name" to reportName)
            
            val results = fetcher.fetchList(
                table = "public.report_configurations",
                fields = "id, name, report_name, description, configuration, is_default",
                filter = "report_name = :report_name",
                orderBy = "is_default DESC, name ASC",
                params = params
            )
            
            results.map { row -> parseConfigurationFromRow(row) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun deleteConfiguration(name: String, reportName: String): Boolean {
        return try {
            val updater = DatabaseManager.getUpdater()
            val sql = "DELETE FROM public.report_configurations WHERE name = :name AND report_type = :report_type"
            val params = mapOf(
                "name" to name,
                "report_name" to reportName
            )
            updater.executeUpdate(sql, params)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun applyConfiguration(configuration: ReportConfiguration, reportState: ReportState, filters: Map<String, Filter>? = null) {
        val configData = configuration.configuration
        
        
        // Zastosuj konfigurację do ReportState
        reportState.visibleColumns.value = configData.visibleColumns.toSet()
        
        // Aktualizuj kolejność kolumn
        reportState.columnKeys.clear()
        reportState.columnKeys.addAll(configData.columnOrder)
        
        // Zastosuj sortowanie
        reportState.sortOrder.value = configData.sortOrder.map { sortConfig ->
            sortConfig.columnName to sortConfig.direction
        }
        
        // Zastosuj filtry
        if (filters != null) {
            val deserializedFilters = deserializeFilterValues(configData.filterValues, reportState.filterValues.value, filters)
            reportState.filterValues.value = deserializedFilters
        }
        
        // Zastosuj rozmiar strony
        reportState.pageSize.value = configData.pageSize
        
        // Reset strony na pierwszą
        reportState.currentPage.value = 0
    }
    
    private fun parseConfigurationFromRow(row: Map<String, Any?>): ReportConfiguration {
        val configJson = row["configuration"] as String
        val configData = json.decodeFromString<ReportConfigurationData>(configJson)
        
        return ReportConfiguration(
            id = row["id"] as Int,
            name = row["name"] as String,
            reportName = row["report_name"] as String,
            description = row["description"] as String?,
            configuration = configData,
            isDefault = row["is_default"] as Boolean
        )
    }
    
    private fun serializeFilterValues(filterValues: Map<String, FilterData<*>>): Map<String, SerializableFilterData> {
        return filterValues.map { (key, filterData) ->
            val serializable = when (filterData) {
                is FilterData.BooleanData -> SerializableFilterData.BooleanFilter(
                    value = filterData.value.value,
                    nullHandling = filterData.nullHandling.value.name
                )
                is FilterData.NumberData<*> -> SerializableFilterData.NumberFilter(
                    filterType = filterData.filterType.value.name,
                    minValue = filterData.minValue.value?.toDouble(),
                    maxValue = filterData.maxValue.value?.toDouble(),
                    nullHandling = filterData.nullHandling.value.name
                )
                is FilterData.StringData -> SerializableFilterData.StringFilter(
                    filterType = filterData.filterType.value.name,
                    value = filterData.value.value,
                    caseSensitive = filterData.caseSensitive.value,
                    nullHandling = filterData.nullHandling.value.name
                )
                is FilterData.EnumData<*> -> SerializableFilterData.EnumFilter(
                    values = filterData.values.value.map { it.toString() },
                    include = filterData.include.value,
                    nullHandling = filterData.nullHandling.value.name
                )
            }
            key to serializable
        }.toMap()
    }
    
    private fun deserializeFilterValues(
        serializedFilters: Map<String, SerializableFilterData>,
        currentFilters: Map<String, FilterData<*>>,
        filters: Map<String, Filter>
    ): Map<String, FilterData<*>> {
        val result = currentFilters.toMutableMap()
        
        serializedFilters.forEach { (key, serializedFilter) ->
            val currentFilter = currentFilters[key]
            if (currentFilter != null) {
                when {
                    serializedFilter is SerializableFilterData.BooleanFilter && currentFilter is FilterData.BooleanData -> {
                        currentFilter.value.value = serializedFilter.value
                        currentFilter.nullHandling.value = NullHandling.valueOf(serializedFilter.nullHandling)
                    }
                    serializedFilter is SerializableFilterData.NumberFilter && currentFilter is FilterData.NumberData<*> -> {
                        currentFilter.filterType.value = NumberFilterDataType.valueOf(serializedFilter.filterType)
                        @Suppress("UNCHECKED_CAST")
                        val numberFilter = currentFilter as FilterData.NumberData<Number>
                        numberFilter.minValue.value = serializedFilter.minValue
                        numberFilter.maxValue.value = serializedFilter.maxValue
                        currentFilter.nullHandling.value = NullHandling.valueOf(serializedFilter.nullHandling)
                    }
                    serializedFilter is SerializableFilterData.StringFilter && currentFilter is FilterData.StringData -> {
                        currentFilter.filterType.value = StringFilterDataType.valueOf(serializedFilter.filterType)
                        currentFilter.value.value = serializedFilter.value
                        currentFilter.caseSensitive.value = serializedFilter.caseSensitive
                        currentFilter.nullHandling.value = NullHandling.valueOf(serializedFilter.nullHandling)
                    }
                    serializedFilter is SerializableFilterData.EnumFilter && currentFilter is FilterData.EnumData<*> -> {
                        // Reset current values first
                        currentFilter.reset()
                        
                        // To deserialize enum values, we need to get them from the filter itself
                        val enumFilter = filters[key] as? EnumFilter<*>
                        if (enumFilter != null) {
                            val enumClass = enumFilter.enumClass
                            val enumConstants = enumClass.java.enumConstants
                            
                            // Restore enum values by matching string representations
                            serializedFilter.values.forEach { enumString ->
                                val matchingEnum = enumConstants.find { it.toString() == enumString }
                                if (matchingEnum != null) {
                                    currentFilter.addValue(matchingEnum)
                                }
                            }
                        }
                        
                        currentFilter.include.value = serializedFilter.include
                        currentFilter.nullHandling.value = NullHandling.valueOf(serializedFilter.nullHandling)
                    }
                }
            }
        }
        
        return result
    }
}