package org.octavius.report

import androidx.compose.runtime.compositionLocalOf
import org.octavius.report.component.ReportState
import org.octavius.report.component.ReportStructure

/**
 * Kontekst danych dla komponentu ReportTable i jego dzieci (nagłówka, wierszy).
 * Hermetyzuje wszystkie potrzebne zależności, upraszczając sygnatury
 * i tworząc jasny kontrakt dla komponentu tabeli.
 */
data class ReportContext(
    val reportStructure: ReportStructure,
    val reportState: ReportState,
    val onEvent: (ReportEvent) -> Unit
)

val LocalReportContext = compositionLocalOf<ReportContext> { error("No ReportContext") }