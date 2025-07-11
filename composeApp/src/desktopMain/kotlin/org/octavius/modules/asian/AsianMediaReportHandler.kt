package org.octavius.modules.asian

import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationStatus
import org.octavius.domain.asian.PublicationType
import org.octavius.localization.Translations
import org.octavius.modules.asian.ui.AsianMediaFormScreen
import org.octavius.navigator.Navigator
import org.octavius.report.Query
import org.octavius.report.ReportAction
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.MultiRowColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportStructure

class AsianMediaReportHandler(val navigator: Navigator) : ReportHandler() {


    override fun createReportStructure(): ReportStructure {
        val query = Query(
            """
            SELECT 
                t.id as title_id,
                t.titles,
                t.language,
                p.publication_type,
                p.status
            FROM titles t
            JOIN publications p ON p.title_id = t.id
            """.trimIndent()
        )

        val columns = mapOf(
            "titles" to MultiRowColumn(
                wrappedColumn = StringColumn(
                    databaseColumnName = "titles",
                    header = Translations.get("games.general.titles"),
                    width = 2f
                ),
                maxVisibleItems = 5
            ),
            "language" to EnumColumn(
                databaseColumnName = "language",
                header = Translations.get("games.general.language"),
                enumClass = PublicationLanguage::class,
                width = 1f
            ),
            "publicationType" to EnumColumn(
                databaseColumnName = "publication_type",
                header = Translations.get("games.general.publicationType"),
                enumClass = PublicationType::class,
                width = 1.5f
            ),
            "status" to EnumColumn(
                databaseColumnName = "status",
                header = Translations.get("games.general.status"),
                enumClass = PublicationStatus::class,
                width = 1.5f
            )
        )

        val rowActions = listOf(
            ReportAction(Translations.get("report.actions.edit")) {
                val id = rowData["title_id"] as? Int
                if (id != null) {
                    navigator.addScreen(
                        AsianMediaFormScreen(
                        entityId = id,
                        onSaveSuccess = { navigator.removeScreen() },
                        onCancel = { navigator.removeScreen() }
                        )
                    )
                }
            }
        )

        return ReportStructure(query, columns, "", "asianMedia", rowActions)
    }
}