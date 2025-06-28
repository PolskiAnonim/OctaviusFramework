package org.octavius.modules.settings

import org.octavius.localization.Translations
import org.octavius.navigator.Navigator
import org.octavius.report.Query
import org.octavius.report.column.type.BooleanColumn
import org.octavius.report.column.type.IntegerColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportStructure

class ApiIntegrationsReportHandler(val navigator: Navigator) : ReportHandler() {

    override var onRowClick: ((Map<String, Any?>) -> Unit)? = { rowData ->
        val id = rowData["id"] as? Int
        if (id != null) {
            // TODO: Open API integration form
        }
    }

    override fun createReportStructure(): ReportStructure {
        val query = Query(
            """
            SELECT id, name, enabled, api_key, endpoint_url, port, last_sync
            FROM api_integrations
            ORDER BY name
            """.trimIndent()
        )

        val columns = mapOf(
            "name" to StringColumn("name", Translations.get("settings.api.columns.name"), filterable = true),
            "enabled" to BooleanColumn("enabled", Translations.get("settings.api.columns.enabled"), filterable = true),
            "api_key" to StringColumn("api_key", Translations.get("settings.api.columns.apiKey"), filterable = false),
            "endpoint_url" to StringColumn(
                "endpoint_url",
                Translations.get("settings.api.columns.endpointUrl"),
                filterable = false
            ),
            "port" to IntegerColumn("port", Translations.get("settings.api.columns.port"), filterable = true),
            "last_sync" to StringColumn(
                "last_sync",
                Translations.get("settings.api.columns.lastSync"),
                filterable = false
            )
        )

        return ReportStructure(query, columns, "", "api_integrations")
    }
}