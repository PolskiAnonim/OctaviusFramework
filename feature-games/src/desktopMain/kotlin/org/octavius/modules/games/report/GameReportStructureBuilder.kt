package org.octavius.modules.games.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import org.octavius.domain.game.GameStatus
import org.octavius.localization.T
import org.octavius.modules.games.form.game.ui.GameFormScreen
import org.octavius.modules.games.form.series.ui.GameSeriesFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.Query
import org.octavius.report.ReportAddAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class GameReportStructureBuilder() : ReportStructureBuilder() {

    override fun getReportName(): String = "games"

    override fun buildQuery(): Query = Query(
        """
            SELECT games.id, games.name AS game_name, series.name AS series_name, games.status
            FROM games
            LEFT JOIN series ON series.id = games.series
            ORDER BY games.name
            """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "game_name" to StringColumn(T.get("games.general.gameName")),
        "series_name" to StringColumn(T.get("games.general.series")),
        "status" to EnumColumn(
            T.get("games.general.status"),
            enumClass = GameStatus::class
        ),
    )

    override fun buildRowActions(): List<ReportRowAction> = listOf(
        ReportRowAction(T.get("report.actions.edit")) {
            val id = rowData["id"] as? Int
            if (id != null) {
                AppRouter.navigateTo(
                    GameFormScreen.create(
                        entityId = id
                    ))
            }
        })

    override fun buildAddActions(): List<ReportAddAction> = listOf(
        ReportAddAction(T.get("games.report.newGame"), Icons.Default.Add) {
            AppRouter.navigateTo(
                GameFormScreen.create(
                )
            )
        },
        ReportAddAction(T.get("games.report.newSeries"), Icons.Default.Add) {
            AppRouter.navigateTo(
                GameSeriesFormScreen.create(
                ))
        })
}