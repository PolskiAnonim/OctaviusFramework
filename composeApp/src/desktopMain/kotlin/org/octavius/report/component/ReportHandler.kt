package org.octavius.report.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.octavius.report.management.ReportConfigurationManager
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

    @Composable
    fun DataFetcher() {
        // Snapshot wszystkich wartości filtrów
        val filterSnapshot = derivedStateOf {
            reportState.filterData.value.flatMap { (_, filter) ->
                filter.getTrackableStates()
            }
        }

        // Aktualne wartości kluczy, które powodują reset paginacji
        val mainKeys = listOf(
            reportState.searchQuery.value,
            reportState.pagination.pageSize.value,
            reportState.sortOrder.value,
            filterSnapshot.value
        )

        val previousMainKeys = remember { mutableStateOf(mainKeys) }

        LaunchedEffect(mainKeys, reportState.pagination.currentPage.value) {
            val mainKeysChanged = previousMainKeys.value != mainKeys

            if (mainKeysChanged) {
                previousMainKeys.value = mainKeys

                if (reportState.pagination.currentPage.value > 0) {
                    reportState.pagination.resetPage()
                    return@LaunchedEffect
                }
            }
            // Dane są pobierane, jeśli:
            // 1. Zmieniła się tylko strona (`mainKeysChanged` jest false).
            // 2. Zmieniły się główne parametry, a aktualna strona to 0 (przez reset bądź taka była)
            // 3. Jest to pierwsze uruchomienie komponentu.
            reportDataManager.fetchData()
        }
    }
}