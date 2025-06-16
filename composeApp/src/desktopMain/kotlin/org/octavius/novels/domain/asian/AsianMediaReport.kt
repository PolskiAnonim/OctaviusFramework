package org.octavius.novels.domain.asian

import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.domain.PublicationType
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.Query
import org.octavius.novels.report.Report
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.StringListColumn
import org.octavius.novels.screens.AsianMediaFormScreen

class AsianMediaReport(val navigator: Navigator) : Report() {

    override fun createQuery(): Query {
        val sql = """
SELECT 
    t.id as title_id,
    t.titles,
    t.language,
    p.publication_type,
    p.status
FROM titles t
JOIN publications p ON p.title_id = t.id 
ORDER BY t.id, p.publication_type
    """.trimIndent()
        return Query(sql)
    }

    override fun createColumns(): Map<String, ReportColumn> {
        return mapOf(
            "titles" to StringListColumn(
                fieldName ="titles",
                header = "Tytuły",
                width = 2f
            ),
            "language" to EnumColumn(
                fieldName = "language",
                header = "Język",
                enumClass = PublicationLanguage::class,
                width = 1f
            ),
            "publicationType" to EnumColumn(
                fieldName = "publication_type",
                header = "Typ publikacji",
                enumClass = PublicationType::class,
                width = 1.5f
            ),
            "status" to EnumColumn(
                fieldName = "status",
                header = "Status",
                enumClass = PublicationStatus::class,
                width = 1.5f
            )
        )
    }



    override var onRowClick: ((Map<String, Any?>) -> Unit)? = { rowData ->
        // Obsługa kliknięcia wiersza, np. otwieranie formularza edycji
        val id = rowData["title_id"] as? Int
        if (id != null) {
            navigator.addScreen(AsianMediaFormScreen(id))
        }
    }
}