package org.octavius.modules.games

import org.octavius.domain.game.GameStatus
import org.octavius.localization.Translations
import org.octavius.modules.games.ui.GameFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.Query
import org.octavius.report.ReportRowAction
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructure
import org.octavius.report.component.ReportStructureBuilder

class GameReportStructureBuilder() : ReportStructureBuilder() {

    override fun buildStructure(): ReportStructure {
        val query = Query(
            """
            SELECT games.id, games.name AS game_name, series.name AS series_name, games.status
            FROM games
            LEFT JOIN series ON series.id = games.series
            ORDER BY games.name
            """.trimIndent()
        )

        val columns = mapOf(
            "game_name" to StringColumn( Translations.get("games.general.gameName")),
            "series_name" to StringColumn(Translations.get("games.general.series")),
            "status" to EnumColumn(
                Translations.get("games.general.status"),
                enumClass = GameStatus::class
            ),
        )

        val rowActions = listOf(
            ReportRowAction(Translations.get("report.actions.edit")) {
                // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
                val id = rowData["id"] as? Int
                if (id != null) {
                    AppRouter.navigateTo(
                        GameFormScreen(
                            entityId = id,
                            onSaveSuccess = { AppRouter.goBack() },
                            onCancel = { AppRouter.goBack() }
                        ))
                }
            }
        )

        return ReportStructure(query, columns, "games", rowActions)
    }

}