package org.octavius.novels.domain.game

import org.octavius.novels.domain.GameStatus
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.Query
import org.octavius.novels.report.Report
import org.octavius.novels.report.components.ReportStructure
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.StringColumn
import org.octavius.novels.screens.GameFormScreen

class GameReport(val navigator: Navigator) : Report() {

    override var onRowClick: ((Map<String, Any?>) -> Unit)? = { rowData ->
        // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
        val id = rowData["id"] as? Int
        if (id != null) {
            navigator.addScreen(GameFormScreen(id))
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
            "name" to StringColumn("game_name", "Nazwa", filterable = true),
            "series" to StringColumn("series_name", "Seria", filterable = true),
            "status" to EnumColumn(
                "status",
                "Status",
                enumClass = GameStatus::class,
                filterable = true
            ),
        )
        
        val columnOrder = listOf("name", "series", "status")
        
        return ReportStructure(query, columns, columnOrder)
    }

}