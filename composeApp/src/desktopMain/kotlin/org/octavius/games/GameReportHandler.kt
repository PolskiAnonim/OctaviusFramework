package org.octavius.games

import org.octavius.domain.game.GameStatus
import org.octavius.localization.Translations
import org.octavius.navigator.Navigator
import org.octavius.report.Query
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportStructure
import org.octavius.ui.screen.form.GameFormScreen

class GameReportHandler(val navigator: Navigator) : ReportHandler() {

    override var onRowClick: ((Map<String, Any?>) -> Unit)? = { rowData ->
        // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
        val id = rowData["id"] as? Int
        if (id != null) {
            navigator.addScreen(GameFormScreen(
            entityId = id,
            onSaveSuccess = { navigator.removeScreen() },
            onCancel = { navigator.removeScreen() }
        ))
        }
    }

    override fun createReportStructure(): ReportStructure {
        val query = Query(
            """
            SELECT games.id, games.name AS game_name, series.name AS series_name, games.status
            FROM games
            LEFT JOIN series ON series.id = games.series
            ORDER BY games.name
            """.trimIndent()
        )
        
        val columns = mapOf(
            "name" to StringColumn("game_name", Translations.get("games.general.gameName"), filterable = true),
            "series" to StringColumn("series_name", Translations.get("games.general.series"), filterable = true),
            "status" to EnumColumn(
                "status",
                Translations.get("games.general.status"),
                enumClass = GameStatus::class,
                filterable = true
            ),
        )
        
        return ReportStructure(query, columns, "")
    }

}