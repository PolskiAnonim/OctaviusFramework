package org.octavius.report.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.octavius.report.ReportEvent
import org.octavius.report.management.ReportConfigurationManager

class ReportHandler(
    coroutineScope: CoroutineScope,
    val reportStructure: ReportStructure
) {

    fun onEvent(event: ReportEvent) {
        eventHandler.onEvent(event)
    }

    // Inicjalizujemy stan początkowy.
    private val _state: MutableStateFlow<ReportState> = MutableStateFlow(ReportState.initial(reportStructure))
    val state = _state.asStateFlow()

    private val dataManager = ReportDataManager(reportStructure)
    private val configManager = ReportConfigurationManager()
    private val eventHandler: ReportEventHandler =
        ReportEventHandler(coroutineScope, dataManager, reportStructure, _state)

    init {
        // Załadowanie domyślnej konfiguracji
        loadDefaultConfiguration()
    }

    //----------------------------------------------STATE---------------------------------------------------------------

    private fun loadDefaultConfiguration() {
        val defaultConfig = configManager.loadDefaultConfiguration(reportStructure.reportName)
        if (defaultConfig != null) {
            onEvent(ReportEvent.ApplyConfiguration(defaultConfig))
        } else {
            onEvent(ReportEvent.Initialize)
        }
    }
}
