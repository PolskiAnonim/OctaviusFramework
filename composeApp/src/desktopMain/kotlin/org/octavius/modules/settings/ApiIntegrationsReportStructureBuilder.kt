package org.octavius.modules.settings

import org.octavius.localization.Translations
import org.octavius.navigator.Navigator
import org.octavius.report.Query
import org.octavius.report.column.type.BooleanColumn
import org.octavius.report.column.type.IntegerColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructure
import org.octavius.report.component.ReportStructureBuilder

class ApiIntegrationsReportStructureBuilder(val navigator: Navigator) : ReportStructureBuilder() {

    override fun buildStructure(): ReportStructure {
        val query = Query(
            """
            SELECT id, name, enabled, api_key, endpoint_url, port, last_sync
            FROM api_integrations
            ORDER BY name
            """.trimIndent()
        )

        val columns = mapOf(
            "name" to StringColumn( Translations.get("settings.api.columns.name"), filterable = true),
            "enabled" to BooleanColumn( Translations.get("settings.api.columns.enabled"), filterable = true),
            "api_key" to StringColumn( Translations.get("settings.api.columns.apiKey"), filterable = false),
            "endpoint_url" to StringColumn(
                Translations.get("settings.api.columns.endpointUrl"),
                filterable = false
            ),
            "port" to IntegerColumn(Translations.get("settings.api.columns.port"), filterable = true),
            "last_sync" to StringColumn(
                Translations.get("settings.api.columns.lastSync"),
                filterable = false
            )
        )

        return ReportStructure(query, columns, "api_integrations")
    }
}