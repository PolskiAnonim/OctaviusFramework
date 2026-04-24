package org.octavius.report.configuration

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import io.github.octaviusframework.db.api.DataAccess
import io.github.octaviusframework.db.api.DataResult
import io.github.octaviusframework.db.api.builder.execute
import io.github.octaviusframework.db.api.builder.toListOf
import io.github.octaviusframework.db.api.builder.toSingleOf
import io.github.octaviusframework.db.api.toDataMap
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class ReportConfigurationManager : KoinComponent {

    val dataAccess: DataAccess by inject()
    fun saveConfiguration(configuration: ReportConfiguration): Boolean {
        val flatValueMap = configuration.toDataMap("id")
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
        ).from(
            "public.report_configurations"
        ).where("report_name = @report_name AND is_default = true").toSingleOf("report_name" to reportName)

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
        ).from(
            "public.report_configurations"
        ).where("report_name = @report_name").orderBy("is_default DESC, name ASC").toListOf("report_name" to reportName)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                emptyList()
            }

            is DataResult.Success<List<ReportConfiguration>> -> result.value
        }
    }

    fun deleteConfiguration(name: String, reportName: String): Boolean {

        val result = dataAccess.deleteFrom("report_configurations")
            .where("name = @name AND report_name = @report_name").execute("name" to name, "report_name" to reportName)

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }

            is DataResult.Success -> {
                result.value > 0
            }
        }
    }
}