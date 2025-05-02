package org.octavius.novels.report

import org.octavius.novels.form.NovelForm
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.column.ReportColumn

import org.octavius.novels.domain.NovelLanguage
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.report.column.type.BooleanColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.IntegerColumn
import org.octavius.novels.report.column.type.StringListColumn

class NovelReport(navigator: Navigator) : Report() {

    override fun createQuery(): Query {
        val sql = """
            SELECT n.id, n.titles, n.novel_type, n.status, n.original_language, 
                   nv.volumes, nv.translated_volumes, nv.original_completed
            FROM novels n
            LEFT JOIN novel_volumes nv ON n.id = nv.id
        """.trimIndent()
        return Query(sql)
    }

    override fun createColumns(): List<ReportColumn> {
        return listOf(StringListColumn(
            name = "titles",
            header = "Tytuły",
            width = 3f
        ),
            EnumColumn(
                name = "status",
                header = "Status",
                enumClass = NovelStatus::class,
                width = 1f
            ),
            EnumColumn(
                name = "originalLanguage",
                header = "Język oryginału",
                enumClass = NovelLanguage::class,
                width = 1f
            ),
            IntegerColumn(
                name = "volumes",
                header = "Tomy",
                width = 0.7f
            ),
            IntegerColumn(
                name = "translatedVolumes",
                header = "Przetłumaczone",
                width = 0.7f
            ),
            BooleanColumn(
                name = "originalCompleted",
                header = "Ukończone",
                width = 0.6f
            ))
    }


    override var onRowClick: ((Map<String, Any?>) -> Unit)? = { rowData ->
        // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
        val id = rowData["id"] as? Int
        if (id != null) {
            navigator.AddScreen(NovelForm(id))
        }
    }
}