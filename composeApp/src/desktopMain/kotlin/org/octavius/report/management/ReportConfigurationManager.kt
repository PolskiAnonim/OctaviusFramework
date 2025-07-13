package org.octavius.report.management

import org.octavius.database.DatabaseManager
import org.octavius.domain.FilterConfig
import org.octavius.domain.SortConfiguration
import org.octavius.report.component.ReportState

class ReportConfigurationManager {

    fun saveConfiguration(
        name: String,
        reportName: String,
        description: String?,
        reportState: ReportState,
        isDefault: Boolean = false
    ): Boolean {
        return try {
            val updater = DatabaseManager.getUpdater()

            val sortOrderList = reportState.sortOrder.map { (columnName, direction) ->
                SortConfiguration(columnName, direction)
            }

            val configSql = """
                INSERT INTO public.report_configurations 
                (name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters)
                VALUES (:name, :report_name, :description, :sort_order, :visible_columns, :column_order, :page_size, :is_default, :filters)
                ON CONFLICT (name, report_name) 
                DO UPDATE SET 
                    description = EXCLUDED.description,
                    sort_order = EXCLUDED.sort_order,
                    visible_columns = EXCLUDED.visible_columns,
                    column_order = EXCLUDED.column_order,
                    page_size = EXCLUDED.page_size,
                    is_default = EXCLUDED.is_default,
                    filters = EXCLUDED.filters,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
            """.trimIndent()

            val filterConfig = reportState.filterData.map { (columnName, filterData) ->
                FilterConfig(columnName, filterData.serialize())
            }

            val configParams = mapOf(
                "name" to name,
                "report_name" to reportName,
                "description" to description,
                "sort_order" to sortOrderList,
                "visible_columns" to reportState.visibleColumns.toList(),
                "column_order" to reportState.columnKeysOrder.toList(),
                "page_size" to reportState.pagination.pageSize,
                "is_default" to isDefault,
                "filters" to filterConfig
            )

            val configId = updater.executeReturning(configSql, configParams, "id") as Int


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
            updater.executeUpdate(sql, params)
            true
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