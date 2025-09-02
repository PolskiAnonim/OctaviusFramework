package org.octavius.modules.games.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import org.octavius.localization.T
import org.octavius.modules.games.form.series.ui.GameSeriesFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.Query
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.IntegerColumn
import org.octavius.report.column.type.LongColumn
import org.octavius.report.column.type.NumberColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class GameSeriesReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "game_series"

    override fun buildQuery(): Query = Query(
        sql = """
        SELECT 
            s.id,
            s.name,
            COALESCE(g.game_count, 0) as game_count
        FROM games.series s
            LEFT JOIN (
                SELECT 
                    series,
                    COUNT(*) as game_count
                FROM games.games
                WHERE series IS NOT NULL
                GROUP BY series
            ) g ON s.id = g.series
        """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "name" to StringColumn(
            header = T.get("games.series.name")
        ),
        "game_count" to LongColumn(
            header = T.get("games.series.gameCount")
        )
    )

    override fun buildDefaultRowAction(): ReportRowAction = ReportRowAction(
        label = T.get("games.form.editSeries"),
        icon = Icons.Default.Edit
    ) {
        val seriesId = rowData["id"] as? Int
        if (seriesId != null) {
            AppRouter.navigateTo(GameSeriesFormScreen.create(seriesId))
        }
    }

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(
            label = T.get("games.form.newSeries"),
            icon = Icons.Default.Add
        ) {
            AppRouter.navigateTo(GameSeriesFormScreen.create())
        }
    )
}