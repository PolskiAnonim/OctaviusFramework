package org.octavius.modules.asian.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationStatus
import org.octavius.domain.asian.PublicationType
import org.octavius.localization.T
import org.octavius.modules.asian.form.ui.AsianMediaFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.Query
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.column.type.asList
import org.octavius.report.component.ReportStructureBuilder

class AsianMediaReportStructureBuilder() : ReportStructureBuilder() {

    override fun getReportName(): String = "asianMedia"

    override fun buildQuery(): Query = Query(
        """
            SELECT 
                t.id as title_id,
                t.titles,
                t.language,
                ARRAY_AGG(p.publication_type ORDER BY p.id) AS publication_type,
                ARRAY_AGG(p.status ORDER BY p.id) as status
            FROM titles t
            JOIN publications p ON p.title_id = t.id
            GROUP BY t.id
            """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "titles" to StringColumn(
            header = T.get("games.general.titles"),
            width = 2f
        ).asList(5),
        "language" to EnumColumn(
            header = T.get("games.general.language"),
            enumClass = PublicationLanguage::class,
            width = 1f
        ),
        "publication_type" to EnumColumn(
            header = T.get("games.general.publicationType"),
            enumClass = PublicationType::class,
            width = 1.5f
        ).asList(9),
        "status" to EnumColumn(
            header = T.get("games.general.status"),
            enumClass = PublicationStatus::class,
            width = 1.5f
        ).asList(9)
    )

    override fun buildDefaultRowAction(): ReportRowAction = ReportRowAction(T.get("report.actions.edit")) {
        val id = rowData["title_id"] as? Int
        if (id != null) {
            AppRouter.navigateTo(
                AsianMediaFormScreen.create(
                    entityId = id
                )
            )
        }
    }

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(T.get("asianMedia.report.newTitle"), Icons.Default.Add) {
            AppRouter.navigateTo(
                AsianMediaFormScreen.create(
                ))
        }
    )
}