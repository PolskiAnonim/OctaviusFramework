package org.octavius.novels.domain.asian

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.domain.PublicationType
import org.octavius.novels.form.ColumnInfo
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
                columnInfo = ColumnInfo("titles", "titles"),
                header = "Tytuły",
                width = 2f
            ),
            "language" to EnumColumn(
                columnInfo = ColumnInfo("titles", "language"),
                header = "Język",
                enumClass = PublicationLanguage::class,
                width = 1f
            ),
            "publicationType" to EnumColumn(
                columnInfo = ColumnInfo("publications", "publication_type"),
                header = "Typ publikacji",
                enumClass = PublicationType::class,
                width = 1.5f
            ),
            "status" to EnumColumn(
                columnInfo = ColumnInfo("publications", "status"),
                header = "Status",
                enumClass = PublicationStatus::class,
                width = 1.5f
            )
        )
    }

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text("Nowa nowelka") },
            onClick = {
                navigator.addScreen(AsianMediaFormScreen())
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
        val id = rowData[ColumnInfo("titles", "title_id")] as? Int
        if (id != null) {
            navigator.addScreen(AsianMediaFormScreen(id))
        }
    }
}