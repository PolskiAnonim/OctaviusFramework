package org.octavius.report.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import org.octavius.report.ReportConfigurationManager
import org.octavius.report.column.ReportColumn

abstract class ReportHandler {

    private val reportStructure: ReportStructure
    private val reportState = ReportState()
    private val reportDataManager = ReportDataManager()

    init {
        reportStructure = this.createReportStructure()
        val filterValues = reportStructure.getAllColumns().filterValues { v -> v.filterable }.mapValues { it.value.getFilterData()!! }
        reportState.filterData.value = filterValues
        reportState.initialize(reportStructure.getAllColumns().keys, reportStructure.reportConfig)
        loadDefaultConfiguration()
        reportDataManager.setReferences(reportStructure, reportState)
    }

    open var onRowClick: ((Map<String, Any?>) -> Unit)? = null

    abstract fun createReportStructure(): ReportStructure

    @Composable
    fun DataFetcher() {
        // Snapshot wszystkich wartości filtrów
        val filterSnapshot = derivedStateOf {
            reportState.filterData.value.flatMap { (_, filter) ->
                filter.getTrackableStates()
            }
        }

        // Efekt pobierający dane po zmianie parametrów
        LaunchedEffect(
            reportState.pagination.currentPage.value,
            reportState.searchQuery.value,
            reportState.pagination.pageSize.value,
            reportState.sortOrder.value,
            filterSnapshot.value
        ) {
            reportDataManager.fetchData()
        }
    }

    fun getColumns(): Map<String, ReportColumn> = reportStructure.getAllColumns()

    fun getReportState(): ReportState = reportState

    private fun loadDefaultConfiguration() {
        val configManager = ReportConfigurationManager()
        val defaultConfig = configManager.loadDefaultConfiguration(reportStructure.reportName)
        
        if (defaultConfig != null) {
            configManager.applyConfiguration(defaultConfig, reportState)
        }
    }

    fun getReportName(): String {
        return reportStructure.reportName
    }

}