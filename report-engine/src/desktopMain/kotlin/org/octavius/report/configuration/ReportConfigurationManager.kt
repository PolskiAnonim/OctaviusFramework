package org.octavius.report.configuration

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.execute
import org.octavius.data.builder.toField
import org.octavius.data.builder.toListOf
import org.octavius.data.builder.toSingleOf
import org.octavius.data.toMap
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionPlanResult
import org.octavius.data.transaction.TransactionPlanResults
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class ReportConfigurationManager : KoinComponent {

    val dataAccess: DataAccess by inject()
    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        val configResult = dataAccess.select("id").from("public.report_configurations")
            .where("name = :name AND report_name = :report_name")
            .toField<Int>("name" to configuration.name, "report_name" to configuration.reportName)

        val configId = when (configResult) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(configResult.error))
                null
            }
            is DataResult.Success<*> -> configResult.value
        }

        val flatValueMap = configuration.toMap(includeNulls = false)

        val plan = TransactionPlan()

        if (configId != null) {
            plan.add(
                dataAccess.update("report_configurations").setValues(flatValueMap).where("id = :id").asStep()
                    .execute(flatValueMap)
            )
        } else {
            plan.add(
                dataAccess.insertInto("report_configurations").values(flatValueMap).asStep().execute(flatValueMap)
            )
        }
        val result = dataAccess.executeTransactionPlan(plan)
        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
            is DataResult.Success<TransactionPlanResult> -> true
        }
    }

    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {
        val result: DataResult<ReportConfiguration?> = dataAccess.select(
            "*"
        ).from("public.report_configurations"
        ).where("report_name = :report_name AND is_default = true").toSingleOf("report_name" to reportName)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                null
            }
            is DataResult.Success<ReportConfiguration?> -> result.value
        }
    }

    fun listConfigurations(reportName: String): List<ReportConfiguration> {
        val result: DataResult<List<ReportConfiguration>> = dataAccess.select(
            "*"
        ).from("public.report_configurations"
        ).where("report_name = :report_name").orderBy("is_default DESC, name ASC").toListOf("report_name" to reportName)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                emptyList()
            }
            is DataResult.Success<List<ReportConfiguration>> -> result.value
        }
    }

    fun deleteConfiguration(name: String, reportName: String): Boolean {
        val plan = TransactionPlan()

        val ref = plan.add(
            dataAccess.deleteFrom("report_configurations").where("name = :name AND report_name = :report_name").asStep().execute("name" to name, "report_name" to reportName)
        )

        val result = dataAccess.executeTransactionPlan(plan)

        when(result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return false
            }
            is DataResult.Success -> {
                val firstStepResult = result.value.get(ref)
                return firstStepResult > 0
            }
        }
    }
}