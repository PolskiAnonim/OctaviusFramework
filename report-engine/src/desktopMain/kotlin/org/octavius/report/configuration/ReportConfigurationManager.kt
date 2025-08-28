package org.octavius.report.configuration

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.BatchStepResults
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DataResult
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.toDatabaseValue
import org.octavius.data.contract.toListOf
import org.octavius.data.contract.toSingleOf
import org.octavius.navigation.AppRouter
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.util.toMap

class ReportConfigurationManager : KoinComponent {

    val fetcher: DataFetcher by inject()
    val batchExecutor: BatchExecutor by inject()
    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        val configResult = fetcher.select("id", from = "public.report_configurations")
            .where("name = :name AND report_name = :report_name")
            .toField<Int>(mapOf("name" to configuration.name, "report_name" to configuration.reportName))

        val configId = when (configResult) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(configResult.error))
                null
            }
            is DataResult.Success<*> -> configResult.value
        }

        val flatValueMap = configuration.toMap()

        val dataMap =
            flatValueMap.filter { (key, _) -> key != "id" }.mapValues { (_, value) -> value.toDatabaseValue() }

        val databaseStep = if (configId != null) {
            DatabaseStep.Update(
                tableName = "report_configurations",
                data = dataMap,
                filter = mapOf("id" to configId.toDatabaseValue())
            )
        } else {
            DatabaseStep.Insert(
                tableName = "public.report_configurations",
                data = dataMap
            )
        }
        val result = batchExecutor.execute(listOf(databaseStep))
        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
            is DataResult.Success<BatchStepResults> -> true
        }
    }

    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {

        val params = mapOf("report_name" to reportName)

        val result: DataResult<ReportConfiguration?> = fetcher.select(
            "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters",
            from = "public.report_configurations"
        ).where("report_name = :report_name AND is_default = true").toSingleOf(params)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                null
            }
            is DataResult.Success<ReportConfiguration?> -> result.value
        }
    }

    fun listConfigurations(reportName: String): List<ReportConfiguration> {
        val params = mapOf("report_name" to reportName)

        val result: DataResult<List<ReportConfiguration>> = fetcher.select(
            "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default, filters",
            from = "public.report_configurations"
        ).where("report_name = :report_name").orderBy("is_default DESC, name ASC").toListOf(params)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                emptyList()
            }
            is DataResult.Success<List<ReportConfiguration>> -> result.value
        }
    }

    fun deleteConfiguration(name: String, reportName: String): Boolean {
        val databaseStep = DatabaseStep.Delete(
            tableName = "report_configurations",
            filter = mapOf("name" to name.toDatabaseValue(), "report_name" to reportName.toDatabaseValue())
        )

        val result = batchExecutor.execute(listOf(databaseStep))

        when(result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return false
            }
            is DataResult.Success -> {
                val firstStepResult = result.value[0]!!
                val firstStepFirstResult = firstStepResult.first()
                return firstStepFirstResult["rows_affected"] as Int > 0
            }
        }
    }
}