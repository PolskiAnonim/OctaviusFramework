package org.octavius.report.configuration

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toListOf
import org.octavius.data.builder.toSingleOf
import org.octavius.data.toMap
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionPlanResults
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class ReportConfigurationManager : KoinComponent {

    val dataAccess: DataAccess by inject()
    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        val configResult = dataAccess.select("id").from("public.report_configurations")
            .where("name = :name AND report_name = :report_name")
            .toField<Int>(mapOf("name" to configuration.name, "report_name" to configuration.reportName))

        val configId = when (configResult) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(configResult.error))
                null
            }
            is DataResult.Success<*> -> configResult.value
        }

        val flatValueMap = configuration.toMap().filterKeys { it != "id" }

        val plan = TransactionPlan(dataAccess)

        if (configId != null) {
            plan.update(
                tableName = "report_configurations",
                data = flatValueMap,
                filter = mapOf("id" to configId)
            )
        } else {
            plan.insert(
                tableName = "report_configurations",
                data = flatValueMap,
            )
        }
        val result = dataAccess.executeTransactionPlan(plan.build())
        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
            is DataResult.Success<TransactionPlanResults> -> true
        }
    }

    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {

        val params = mapOf("report_name" to reportName)

        val result: DataResult<ReportConfiguration?> = dataAccess.select(
            "*"
        ).from("public.report_configurations"
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

        val result: DataResult<List<ReportConfiguration>> = dataAccess.select(
            "*"
        ).from("public.report_configurations"
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
        val plan = TransactionPlan(dataAccess)
        plan.delete("report_configurations", mapOf("name" to name, "report_name" to reportName))

        val result = dataAccess.executeTransactionPlan(plan.build())

        when(result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return false
            }
            is DataResult.Success -> {
                val firstStepResult = result.value[0]!!
                val firstStepFirstResult = firstStepResult.first()
                return firstStepFirstResult["result"] as Int > 0
            }
        }
    }
}