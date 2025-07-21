package org.octavius.report.configuration

import org.octavius.database.DatabaseManager
import org.octavius.domain.FilterConfig
import org.octavius.domain.SortConfiguration
import org.octavius.form.SaveOperation

class ReportConfigurationManager {

    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val updater = DatabaseManager.getUpdater()

            val existingConfigId = fetcher.fetchField(
                table = "public.report_configurations",
                field = "id",
                filter = "name = :name AND report_name = :report_name",
                params = mapOf("name" to configuration.name, "report_name" to configuration.reportName)
            ) as? Int

            val configData = configuration.configuration

            val dataMap: Map<String, Any?> = mapOf(
                "name" to configuration.name,
                "report_name" to configuration.reportName,
                "description" to configuration.description,
                "sort_order" to configData.sortOrder,
                "visible_columns" to configData.visibleColumns,
                "column_order" to configData.columnOrder,
                "page_size" to configData.pageSize,
                "is_default" to configuration.isDefault,
                "filters" to configData.filters
            )

            val operation = if (existingConfigId != null) {
                SaveOperation.Update(
                    tableName = "public.report_configurations",
                    data = dataMap,
                    id = existingConfigId
                )
            } else {
                SaveOperation.Insert(
                    tableName = "public.report_configurations",
                    data = dataMap,
                    returningId = false
                )
            }

            updater.updateDatabase(listOf(operation))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val params = mapOf("report_name" to reportName)

            val result = fetcher.fetchRowOrNull(
                table = "public.report_configurations",
                columns = "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters",
                filter = "report_name = :report_name AND is_default = true",
                params = params
            )

            result?.let { row ->
                parseConfigurationFromNewRow(row)
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
                columns = "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters",
                filter = "report_name = :report_name",
                orderBy = "is_default DESC, name ASC",
                params = params
            )

            results.map { row -> parseConfigurationFromNewRow(row) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun deleteConfiguration(name: String, reportName: String): Boolean {
        return try {
            val updater = DatabaseManager.getUpdater()
            val sql = "DELETE FROM public.report_configurations WHERE name = :name AND report_name = :report_name"
            val params = mapOf(
                "name" to name,
                "report_name" to reportName
            )
            updater.executeUpdate(sql, params) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseConfigurationFromNewRow(row: Map<String, Any?>): ReportConfiguration {
        val sortOrder = row["sort_order"] as List<SortConfiguration>
        val visibleColumns = row["visible_columns"] as List<String>
        val columnOrder = row["column_order"] as List<String>
        val filters = row["filters"] as List<FilterConfig>

        val configData = ReportConfigurationData(
            visibleColumns = visibleColumns,
            columnOrder = columnOrder,
            sortOrder = sortOrder,
            filters = filters,
            pageSize = row["page_size"] as Int
        )

        return ReportConfiguration(
            id = row["id"] as Int,
            name = row["name"] as String,
            reportName = row["report_name"] as String,
            description = row["description"] as String?,
            configuration = configData,
            isDefault = row["is_default"] as Boolean
        )
    }
}