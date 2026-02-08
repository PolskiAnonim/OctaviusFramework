package org.octavius.modules.activity.report

import org.octavius.data.QueryFragment
import org.octavius.localization.Tr
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.InstantColumn
import org.octavius.report.column.type.IntegerColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class ActivityLogReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "activity_log"

    override fun buildQuery(): QueryFragment = QueryFragment(
        sql = """
            SELECT
                al.id,
                al.window_title,
                al.process_name,
                al.started_at,
                al.ended_at,
                al.duration_seconds,
                c.name as category_name
            FROM activity_tracker.activity_log al
            LEFT JOIN activity_tracker.categories c ON al.category_id = c.id
        """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "window_title" to StringColumn(
            header = Tr.ActivityTracker.Report.windowTitle()
        ),
        "process_name" to StringColumn(
            header = Tr.ActivityTracker.Report.processName()
        ),
        "started_at" to InstantColumn(
            header = Tr.ActivityTracker.Report.startedAt()
        ),
        "ended_at" to InstantColumn(
            header = Tr.ActivityTracker.Report.endedAt()
        ),
        "duration_seconds" to IntegerColumn(
            header = Tr.ActivityTracker.Report.durationSeconds()
        ),
        "category_name" to StringColumn(
            header = Tr.ActivityTracker.Report.categoryName()
        )
    )
}
