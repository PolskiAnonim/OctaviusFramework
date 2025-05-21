package org.octavius.novels.domain.novel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.Query
import org.octavius.novels.report.Report
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.column.type.BooleanColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.IntegerColumn
import org.octavius.novels.report.column.type.StringListColumn

class NovelReport(val navigator: Navigator) : Report() {

    override fun createQuery(): Query {
        val sql = """
            SELECT n.id, n.titles, n.novel_type, n.status, n.original_language, 
                   nv.volumes, nv.translated_volumes, nv.original_completed
            FROM novels n
            LEFT JOIN novel_volumes nv ON n.id = nv.id
        """.trimIndent()
        return Query(sql)
    }

    override fun createColumns(): Map<String, ReportColumn> {
        return mapOf(
            "titles" to StringListColumn(
                columnInfo = ColumnInfo("novels", "titles"),
                header = "Tytuły",
                width = 3f
            ),
            "status" to EnumColumn(
                columnInfo = ColumnInfo("novels", "status"),
                header = "Status",
                enumClass = PublicationStatus::class,
                width = 1f
            ),
            "originalLanguage" to EnumColumn(
                columnInfo = ColumnInfo("novels", "original_language"),
                header = "Język oryginału",
                enumClass = PublicationLanguage::class,
                width = 1f
            ),
            "volumes" to IntegerColumn(
                columnInfo = ColumnInfo("novel_volumes", "volumes"),
                header = "Tomy",
                width = 0.7f
            ),
            "translatedVolumes" to IntegerColumn(
                columnInfo = ColumnInfo("novel_volumes", "translated_volumes"),
                header = "Przetłumaczone",
                width = 0.7f
            ),
            "originalCompleted" to BooleanColumn(
                columnInfo = ColumnInfo("novel_volumes", "original_completed"),
                header = "Ukończona",
                width = 0.6f
            )
        )
    }

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text("Nowa nowelka") },
            onClick = {
                navigator.addScreen(NovelForm())
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        )
        // Tutaj możesz dodać więcej opcji, jeśli potrzebujesz
    }


    override var onRowClick: ((Map<ColumnInfo, Any?>) -> Unit)? = { rowData ->
        // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
        val id = rowData[ColumnInfo("novels", "id")] as? Int
        if (id != null) {
            navigator.addScreen(NovelForm(id))
        }
    }
}