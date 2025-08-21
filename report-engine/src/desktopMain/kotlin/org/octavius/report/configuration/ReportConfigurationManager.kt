package org.octavius.report.configuration

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.toDatabaseValue
import org.octavius.data.contract.toListOf
import org.octavius.data.contract.toSingleOf
import org.octavius.util.toMap

class ReportConfigurationManager : KoinComponent {

    val fetcher: DataFetcher by inject()
    val batchExecutor: BatchExecutor by inject()
    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        return try {
            val existingConfigId = fetcher.select("id", from = "public.report_configurations").where("name = :name AND report_name = :report_name")
                .toField<Int>(mapOf("name" to configuration.name, "report_name" to configuration.reportName))

            val flatValueMap = configuration.toMap()

            val dataMap =
                flatValueMap.filter { (key, _) -> key != "id" }.mapValues { (_, value) -> value.toDatabaseValue() }

            val databaseStep = if (existingConfigId != null) {
                DatabaseStep.Update(
                    tableName = "report_configurations",
                    data = dataMap,
                    filter = mapOf("id" to existingConfigId.toDatabaseValue())
                )
            } else {
                DatabaseStep.Insert(
                    tableName = "public.report_configurations",
                    data = dataMap
                )
            }

            batchExecutor.execute(listOf(databaseStep))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {
        return try {
            val params = mapOf("report_name" to reportName)

            fetcher.select(
                "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters",
                from = "public.report_configurations"
            ).where("report_name = :report_name AND is_default = true").toSingleOf(params)
        } catch (e: Exception) {
            null
        }
    }

    fun listConfigurations(reportName: String): List<ReportConfiguration> {
        return try {
            val params = mapOf("report_name" to reportName)

            fetcher.select(
                "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters",
                from = "public.report_configurations"
            ).where("report_name = :report_name").orderBy("is_default DESC, name ASC").toListOf(params)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteConfiguration(name: String, reportName: String): Boolean {
        return try {
            val databaseStep = DatabaseStep.Delete(
                tableName = "report_configurations",
                filter = mapOf("name" to name.toDatabaseValue(), "report_name" to reportName.toDatabaseValue())
            )

            batchExecutor.execute(listOf(databaseStep))[0]!![0]["rows_affected"] as Int > 0
        } catch (e: Exception) {
            false
        }
    }
}