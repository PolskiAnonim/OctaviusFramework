package org.octavius.modules.games.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import org.octavius.data.QueryFragment
import org.octavius.domain.game.GameStatus
import org.octavius.localization.Tr
import org.octavius.modules.games.form.game.ui.GameFormScreen
import org.octavius.modules.games.form.series.ui.GameSeriesFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.column.type.asList
import org.octavius.report.component.ReportStructureBuilder

class GameReportStructureBuilder() : ReportStructureBuilder() {

    override fun getReportName(): String = "games"

    override fun buildQuery(): QueryFragment {
        val query = dataAccess.select(
            "games.id",
            "games.name AS game_name",
            "series.name AS series_name",
            "games.status",
            """COALESCE(
                (SELECT array_agg(c.name ORDER BY c.name)
                 FROM games.categories_to_games ctg
                 JOIN games.categories c ON c.id = ctg.category_id
                 WHERE ctg.game_id = games.id),
                ARRAY[]::text[]
            ) AS categories"""
        ).from("games LEFT JOIN series ON series.id = games.series").toSql()
        return QueryFragment(query)
    }

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "game_name" to StringColumn(Tr.Games.General.gameName()),
        "series_name" to StringColumn(Tr.Games.General.series()),
        "status" to EnumColumn(
            Tr.Games.General.status(),
            enumClass = GameStatus::class
        ),
        "categories" to StringColumn(Tr.Games.Form.category(2)).asList(),
    )

    override fun buildRowActions(): List<ReportRowAction> = listOf(
        ReportRowAction(Tr.Report.Actions.edit()) {
            val id = rowData["id"] as? Int
            if (id != null) {
                AppRouter.navigateTo(
                    GameFormScreen.create(
                        entityId = id
                    ))
            }
        })

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(Tr.Games.Report.newGame(), Icons.Default.Add) {
            AppRouter.navigateTo(
                GameFormScreen.create(
                )
            )
        },
        ReportMainAction(Tr.Games.Report.newSeries(), Icons.Default.Add) {
            AppRouter.navigateTo(
                GameSeriesFormScreen.create(
                ))
        })
}