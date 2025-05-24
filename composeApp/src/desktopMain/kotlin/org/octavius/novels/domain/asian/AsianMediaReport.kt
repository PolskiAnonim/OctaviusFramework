package org.octavius.novels.domain.asian

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.report.Query
import org.octavius.novels.report.Report
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.StringListColumn

class AsianMediaReport(val navigator: Navigator) : Report() {

    override fun createQuery(): Query {
        val sql = """
SELECT 
    t.id, 
    t.titles,
    t.language,
    CASE 
        WHEN bool_or(p.status = 'COMPLETED') THEN 'COMPLETED'::publication_status
        WHEN bool_or(p.status = 'PLAN_TO_READ') THEN 'PLAN_TO_READ'::publication_status
        ELSE 'NOT_READING'::publication_status
    END as status
FROM titles t
JOIN publications p ON p.title_id = t.id 
GROUP BY t.id, t.titles
        """.trimIndent()
        return Query(sql)
    }

    override fun createColumns(): Map<String, ReportColumn> {
        return mapOf(
            "titles" to StringListColumn(
                columnInfo = ColumnInfo("titles", "titles"),
                header = "Tytuły",
                width = 3f
            ),
            "status" to EnumColumn(
                columnInfo = ColumnInfo("", "status"),
                header = "Status",
                enumClass = PublicationStatus::class,
                width = 1f,
                filterable = false
            ),
            "originalLanguage" to EnumColumn(
                columnInfo = ColumnInfo("titles", "language"),
                header = "Język oryginału",
                enumClass = PublicationLanguage::class,
                width = 1f
            )
        )
    }

    @Composable
    override fun AddMenu() {
        DropdownMenuItem(
            text = { Text("Nowa nowelka") },
            onClick = {
                navigator.addScreen(AsianMediaForm())
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
        val id = rowData[ColumnInfo("titles", "id")] as? Int
        if (id != null) {
            navigator.addScreen(AsianMediaForm(id))
        }
    }
}