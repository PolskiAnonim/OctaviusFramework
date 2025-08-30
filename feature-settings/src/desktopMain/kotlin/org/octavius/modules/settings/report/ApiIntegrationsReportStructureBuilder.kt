package org.octavius.modules.settings.report

import org.octavius.localization.T
import org.octavius.report.Query
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.BooleanColumn
import org.octavius.report.column.type.IntegerColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class ApiIntegrationsReportStructureBuilder() : ReportStructureBuilder() {

    override fun getReportName(): String = "apiIntegrations"

    override fun buildQuery(): Query = Query(
        """
            SELECT id, name, enabled, api_key, endpoint_url, port, last_sync
            FROM api_integrations
            ORDER BY name
            """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "name" to StringColumn(T.get("settings.api.columns.name"), filterable = true),
        "enabled" to BooleanColumn(T.get("settings.api.columns.enabled"), filterable = true),
        "api_key" to StringColumn(T.get("settings.api.columns.apiKey"), filterable = false),
        "endpoint_url" to StringColumn(
            T.get("settings.api.columns.endpointUrl"),
            filterable = false
        ),
        "port" to IntegerColumn(T.get("settings.api.columns.port"), filterable = true),
        "last_sync" to StringColumn(
            T.get("settings.api.columns.lastSync"),
            filterable = false
        )
    )
}