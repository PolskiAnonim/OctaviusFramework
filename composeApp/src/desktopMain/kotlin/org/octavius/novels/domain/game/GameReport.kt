package org.octavius.novels.domain.game

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import org.octavius.novels.domain.GameStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.Query
import org.octavius.novels.report.Report
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.StringColumn

class GameReport(val navigator: Navigator) : Report() {
    override fun createQuery(): Query {
        val sql = """
            SELECT g.id, g.name, g_series.name, g.status
            FROM games g
            LEFT JOIN game_series g_series ON g_series.id = g.series
        """.trimIndent()
        return Query(sql)
    }

    override fun createColumns(): Map<String, ReportColumn> {
        return mapOf(
            "name" to StringColumn(ColumnInfo("games", "name"), "Nazwa", filterable = true),
            "series" to StringColumn(ColumnInfo("game_series", "name"), "Seria", filterable = true),
            "status" to EnumColumn(
                ColumnInfo("games", "status"),
                "Status",
                enumClass = GameStatus::class,
                filterable = true
            ),
        )
    }
}