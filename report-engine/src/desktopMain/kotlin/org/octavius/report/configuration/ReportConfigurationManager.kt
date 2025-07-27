package org.octavius.report.configuration

import org.octavius.database.DatabaseManager
import org.octavius.database.DatabaseStep
import org.octavius.database.DatabaseValue
import org.octavius.domain.FilterConfig
import org.octavius.domain.SortConfiguration

class ReportConfigurationManager {

    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val transactionManager = DatabaseManager.getTransactionManager()

            val existingConfigId = fetcher.fetchField(
                table = "public.report_configurations",
                field = "id",
                filter = "name = :name AND report_name = :report_name",
                params = mapOf("name" to configuration.name, "report_name" to configuration.reportName)
            ) as? Int

            val configData = configuration.configuration

            val dataMap: Map<String, DatabaseValue> = mapOf(
                "name" to DatabaseValue.Value(configuration.name),
                "report_name" to DatabaseValue.Value(configuration.reportName),
                "description" to DatabaseValue.Value(configuration.description),
                "sort_order" to DatabaseValue.Value(configData.sortOrder),
                "visible_columns" to DatabaseValue.Value(configData.visibleColumns),
                "column_order" to DatabaseValue.Value(configData.columnOrder),
                "page_size" to DatabaseValue.Value(configData.pageSize),
                "is_default" to DatabaseValue.Value(configuration.isDefault),
                "filters" to DatabaseValue.Value(configData.filters)
            )

            val databaseStep = if (existingConfigId != null) {
                DatabaseStep.Update(
                    tableName = "report_configurations",
                    data = dataMap,
                    filter = mapOf("id" to DatabaseValue.Value(existingConfigId))
                )
            } else {
                DatabaseStep.Insert(
                    tableName = "public.report_configurations",
                    data = dataMap,
                    returning = listOf()
                )
            }

            transactionManager.execute(listOf(databaseStep))
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
            val transactionManager = DatabaseManager.getTransactionManager()

            val databaseStep = DatabaseStep.Delete(
                tableName = "report_configurations",
                filter = mapOf("name" to DatabaseValue.Value(name), "report_name" to DatabaseValue.Value(reportName))
            )

            transactionManager.execute(listOf(databaseStep))[0]!![0]["rows_affected"] as Int > 0
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