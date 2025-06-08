package org.octavius.novels.domain.game

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.novels.domain.GameStatus
import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.Query
import org.octavius.novels.report.Report
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.StringColumn
import org.octavius.novels.screens.GameFormScreen
import org.octavius.novels.screens.GameSeriesFormScreen

class GameReport(val navigator: Navigator) : Report() {
    override fun createQuery(): Query {
        val sql = """
            SELECT games.id, games.name, series.name, games.status
            FROM games
            LEFT JOIN series ON series.id = games.series
            ORDER BY games.name
        """.trimIndent()
        return Query(sql)
    }

    override var onRowClick: ((Map<ColumnInfo, Any?>) -> Unit)? = { rowData ->
        // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
        val id = rowData[ColumnInfo("games", "id")] as? Int
        if (id != null) {
            navigator.addScreen(GameFormScreen(id))
        }
    }

    override fun createColumns(): Map<String, ReportColumn> {
        return mapOf(
            "name" to StringColumn(ColumnInfo("games", "name"), "Nazwa", filterable = true),
            "series" to StringColumn(ColumnInfo("series", "name"), "Seria", filterable = true),
            "status" to EnumColumn(
                ColumnInfo("games", "status"),
                "Status",
                enumClass = GameStatus::class,
                filterable = true
            ),
        )
    }

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text("Nowa gra") },
            onClick = {
                navigator.addScreen(GameFormScreen())
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Nowa seria") },
            onClick = {
                navigator.addScreen(GameSeriesFormScreen())
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        )
    }
}