package org.octavius.report

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.octavius.database.DatabaseManager
import org.octavius.report.component.ReportState

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
    
    fun applyConfiguration(configuration: ReportConfiguration, reportState: ReportState) {
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
        
        // Zastosuj filtry - na razie pomijamy serialization filtrów, to jest skomplikowane
        // reportState.filterValues.value = deserializeFilterValues(configData.filterValues, reportState.filterValues.value)
        
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
    
    private fun serializeFilterValues(filterValues: Map<String, FilterData<*>>): Map<String, String> {
        // Na razie zwracamy puste mapy, serialization filtrów to osobny temat
        return emptyMap()
    }
}