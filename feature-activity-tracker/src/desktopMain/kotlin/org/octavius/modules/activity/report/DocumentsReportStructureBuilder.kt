package org.octavius.modules.activity.report

import org.octavius.data.QueryFragment
import org.octavius.localization.Tr
import org.octavius.modules.activity.domain.DocumentType
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.InstantColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class DocumentsReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "activity_documents"

    override fun buildQuery(): QueryFragment = QueryFragment(
        sql = """
            SELECT
                d.id,
                d.path,
                d.title,
                d.type,
                d.timestamp,
                al.window_title,
                al.process_name
            FROM activity_tracker.documents d
            LEFT JOIN activity_tracker.activity_log al ON d.activity_id = al.id
        """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "path" to StringColumn(
            header = Tr.ActivityTracker.Report.documentPath()
        ),
        "title" to StringColumn(
            header = Tr.ActivityTracker.Report.documentTitle()
        ),
        "type" to EnumColumn(
            header = Tr.ActivityTracker.Report.documentType(),
            enumClass = DocumentType::class
        ),
        "timestamp" to InstantColumn(
            header = Tr.ActivityTracker.Report.timestamp()
        ),
        "window_title" to StringColumn(
            header = Tr.ActivityTracker.Report.windowTitle()
        ),
        "process_name" to StringColumn(
            header = Tr.ActivityTracker.Report.processName()
        )
    )
}
