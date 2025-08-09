package org.octavius.modules.asian.report.ui

import org.octavius.localization.Translations
import org.octavius.modules.asian.report.AsianMediaReportStructureBuilder
import org.octavius.report.component.ReportScreen

class AsianMediaReportScreen {
    companion object {
        fun create(): ReportScreen {
            val title = Translations.get("asianMedia.report.title")
            val builder = AsianMediaReportStructureBuilder()
            return ReportScreen(title, builder)
        }
    }
}