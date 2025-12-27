package org.octavius.report.configuration

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.execute
import org.octavius.data.builder.toListOf
import org.octavius.data.builder.toSingleOf
import org.octavius.data.toMap
import org.octavius.data.transaction.TransactionPlan
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class ReportConfigurationManager : KoinComponent {

    val dataAccess: DataAccess by inject()
    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        val flatValueMap = configuration.toMap("id")
        val result = dataAccess.insertInto("report_configurations")
            .values(flatValueMap).onConflict {
                onColumns("name", "report_name")
                doUpdate(
                    "description" to "EXCLUDED.description",
                    "sort_order" to "EXCLUDED.sort_order",
                    "visible_columns" to "EXCLUDED.visible_columns",
                    "column_order" to "EXCLUDED.column_order",
                    "page_size" to "EXCLUDED.page_size",
                    "is_default" to "EXCLUDED.is_default",
                    "filters" to "EXCLUDED.filters"
                )
            }.execute(flatValueMap)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
            is DataResult.Success<Int> -> true
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

        return when(val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }

            is DataResult.Success -> {
                val firstStepResult = result.value.get(ref)
                firstStepResult > 0
            }
        }
    }
}