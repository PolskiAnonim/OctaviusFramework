package org.octavius.report.management

import org.octavius.database.DatabaseManager
import org.octavius.domain.NullHandling
import org.octavius.domain.NumberFilterDataType
import org.octavius.domain.SortConfiguration
import org.octavius.domain.StringFilterDataType
import org.octavius.report.component.ReportState
import org.octavius.report.filter.data.FilterData
import org.octavius.report.filter.data.type.BooleanFilterData
import org.octavius.report.filter.data.type.EnumFilterData
import org.octavius.report.filter.data.type.NumberFilterData
import org.octavius.report.filter.data.type.StringFilterData
import org.octavius.util.Converters

class ReportConfigurationManager {

    val numberTypes: Map<String, String> = mapOf(
        Int::class.simpleName!! to "INT4",
        Long::class.simpleName!! to "INT8",
        Float::class.simpleName!! to "FLOAT4",
        Double::class.simpleName!! to "FLOAT8",
        "INT4" to Int::class.simpleName!!,
        "INT8" to Long::class.simpleName!!,
        "FLOAT4" to Float::class.simpleName!!,
        "FLOAT8" to Double::class.simpleName!!
    )

    fun saveConfiguration(
        name: String,
        reportName: String,
        description: String?,
        reportState: ReportState,
        isDefault: Boolean = false
    ): Boolean {
        return try {
            val updater = DatabaseManager.getUpdater()

            val sortOrderList = reportState.sortOrder.value.map { (columnName, direction) ->
                SortConfiguration(columnName, direction)
            }

            val configSql = """
                INSERT INTO public.report_configurations 
                (name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default)
                VALUES (:name, :report_name, :description, :sort_order, :visible_columns, :column_order, :page_size, :is_default)
                ON CONFLICT (name, report_name) 
                DO UPDATE SET 
                    description = EXCLUDED.description,
                    sort_order = EXCLUDED.sort_order,
                    visible_columns = EXCLUDED.visible_columns,
                    column_order = EXCLUDED.column_order,
                    page_size = EXCLUDED.page_size,
                    is_default = EXCLUDED.is_default,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
            """.trimIndent()

            val configParams = mapOf(
                "name" to name,
                "report_name" to reportName,
                "description" to description,
                "sort_order" to sortOrderList,
                "visible_columns" to reportState.visibleColumns.value.toList(),
                "column_order" to reportState.columnKeys.toList(),
                "page_size" to reportState.pagination.pageSize.value,
                "is_default" to isDefault
            )

            val configId = updater.executeReturning(configSql, configParams, "id") as Int

            // Delete existing filter configurations for this config
            val deleteFiltersSql = "DELETE FROM public.report_filter_configs WHERE report_config_id = :config_id"
            updater.executeUpdate(deleteFiltersSql, mapOf("config_id" to configId))

            // Save filter configurations
            saveFilterConfigurations(configId, reportState.filterData.value)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadDefaultConfiguration(reportName: String): ReportConfiguration? {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val params = mapOf("report_name" to reportName)

            val result = fetcher.fetchRowOrNull(
                table = "public.report_configurations",
                columns = "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default",
                filter = "report_name = :report_name AND is_default = true",
                params = params
            )

            result?.let { row ->
                parseConfigurationFromNewRow(row)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listConfigurations(reportName: String): List<ReportConfiguration> {
        return try {
            val fetcher = DatabaseManager.getFetcher()
            val params = mapOf("report_name" to reportName)

            val results = fetcher.fetchList(
                table = "public.report_configurations",
                columns = "id, name, report_name, description, sort_order, visible_columns, column_order, page_size, is_default",
                filter = "report_name = :report_name",
                orderBy = "is_default DESC, name ASC",
                params = params
            )

            results.map { row -> parseConfigurationFromNewRow(row) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun deleteConfiguration(name: String, reportName: String): Boolean {
        return try {
            val updater = DatabaseManager.getUpdater()
            val sql = "DELETE FROM public.report_configurations WHERE name = :name AND report_name = :report_name"
            val params = mapOf(
                "name" to name,
                "report_name" to reportName
            )
            updater.executeUpdate(sql, params)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun applyConfiguration(configuration: ReportConfiguration, reportState: ReportState) {
        val configData = configuration.configuration

        // Zastosuj konfigurację do ReportState
        reportState.visibleColumns.value = configData.visibleColumns.toSet()

        // Aktualizuj kolejność kolumn
        reportState.columnKeys.clear()
        reportState.columnKeys.addAll(configData.columnOrder)

        // Zastosuj sortowanie
        reportState.sortOrder.value = configData.sortOrder.map { sortConfig ->
            sortConfig.columnName to sortConfig.sortDirection
        }

        // Zastosuj rozmiar strony
        reportState.pagination.pageSize.value = configData.pageSize

        // Reset strony na pierwszą
        reportState.pagination.resetPage()

        // Load and apply filters from database
        if (configuration.id != null) {
            loadAndApplyFilters(configuration.id, reportState)
        }
    }

    private fun loadAndApplyFilters(configId: Int, reportState: ReportState) {
        try {
            val fetcher = DatabaseManager.getFetcher()
            val filterConfigs = fetcher.fetchList(
                table = "public.report_filter_configs",
                columns = "column_name, filter_type, null_handling, boolean_value, string_filter_type, string_value, case_sensitive, number_filter_type, number_type_name, min_value, max_value, enum_type_name, enum_values, include_enum",
                filter = "report_config_id = :config_id",
                params = mapOf("config_id" to configId)
            )

            filterConfigs.forEach { filterConfig ->
                val columnName = filterConfig["column_name"] as String
                val currentFilter = reportState.filterData.value[columnName]

                if (currentFilter != null) {
                    applyFilterConfiguration(filterConfig, currentFilter)
                }
            }

            // Trigger refresh
            reportState.filterData.value = reportState.filterData.value.toMap()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyFilterConfiguration(filterConfig: Map<String, Any?>, filter: FilterData) {
        when (filter) {
            is BooleanFilterData -> applyBooleanFilterConfig(filterConfig, filter)
            is StringFilterData -> applyStringFilterConfig(filterConfig, filter)
            is NumberFilterData<*> -> applyNumberFilterConfig(filterConfig, filter)
            is EnumFilterData<*> -> applyEnumFilterConfig(filterConfig, filter)
        }
    }

    private fun applyBooleanFilterConfig(config: Map<String, Any?>, filter: BooleanFilterData) {
        filter.value.value = config["boolean_value"] as? Boolean
        filter.nullHandling.value = config["null_handling"] as NullHandling
    }

    private fun applyStringFilterConfig(config: Map<String, Any?>, filter: StringFilterData) {
        filter.filterType.value = config["string_filter_type"] as StringFilterDataType
        filter.value.value = config["string_value"] as String
        filter.caseSensitive.value = config["case_sensitive"] as Boolean
        filter.nullHandling.value = config["null_handling"] as NullHandling
    }

    private fun applyNumberFilterConfig(config: Map<String, Any?>, filter: NumberFilterData<*>) {
        filter.filterType.value = config["number_filter_type"] as NumberFilterDataType

        val numberTypeName = config["number_type_name"] as String
        val converter = DatabaseManager.getConverter()

        val minValueText = config["min_value"] as? String
        val maxValueText = config["max_value"] as? String

        @Suppress("UNCHECKED_CAST")
        val typedFilter = filter as NumberFilterData<Number>

        if (minValueText != null) {
            typedFilter.minValue.value = converter.convertToDomainType(minValueText, numberTypeName) as? Number
        }

        if (maxValueText != null) {
            typedFilter.maxValue.value = converter.convertToDomainType(maxValueText, numberTypeName) as? Number
        }

        filter.nullHandling.value = config["null_handling"] as NullHandling
    }

    private fun applyEnumFilterConfig(config: Map<String, Any?>, filter: EnumFilterData<*>) {
        filter.resetFilter()

        val enumTypeName = config["enum_type_name"] as String
        val enumValuesList = config["enum_values"] as List<String>


        val converter = DatabaseManager.getConverter()

        val convertedEnums = enumValuesList.map { enumValue ->
            converter.convertToDomainType(enumValue, enumTypeName)
        }

        @Suppress("UNCHECKED_CAST")
        val typedFilter = filter as EnumFilterData<Any>
        typedFilter.values.addAll(convertedEnums as List<Any>)

        filter.include.value = config["include_enum"] as Boolean
        filter.nullHandling.value = config["null_handling"] as NullHandling
    }

    private fun saveFilterConfigurations(configId: Int, filterValues: Map<String, FilterData>) {
        val updater = DatabaseManager.getUpdater()

        filterValues.forEach { (columnName, filterData) ->
            when (filterData) {
                is BooleanFilterData -> {
                    if (!filterData.isActive()) return@forEach
                    val filterSql = """
                        INSERT INTO public.report_filter_configs 
                        (report_config_id, column_name, null_handling, filter_type, boolean_value)
                        VALUES (:config_id, :column_name, :null_handling, 'BOOLEAN', :boolean_value)
                    """.trimIndent()

                    val params = mapOf(
                        "config_id" to configId,
                        "column_name" to columnName,
                        "null_handling" to filterData.nullHandling.value,
                        "boolean_value" to filterData.value.value
                    )

                    updater.executeUpdate(filterSql, params)
                }

                is StringFilterData -> {
                    if (!filterData.isActive()) return@forEach
                    val filterSql = """
                        INSERT INTO public.report_filter_configs 
                        (report_config_id, column_name, null_handling, filter_type, string_filter_type, string_value, case_sensitive)
                        VALUES (:config_id, :column_name, :null_handling, 'STRING', :string_filter_type, :string_value, :case_sensitive)
                    """.trimIndent()

                    val params = mapOf(
                        "config_id" to configId,
                        "column_name" to columnName,
                        "null_handling" to filterData.nullHandling.value,
                        "string_filter_type" to filterData.filterType.value,
                        "string_value" to filterData.value.value,
                        "case_sensitive" to filterData.caseSensitive.value
                    )

                    updater.executeUpdate(filterSql, params)
                }

                is NumberFilterData<*> -> {
                    if (!filterData.isActive()) return@forEach
                    val filterSql = """
                        INSERT INTO public.report_filter_configs 
                        (report_config_id, column_name, null_handling, filter_type, number_filter_type, number_type_name, min_value, max_value)
                        VALUES (:config_id, :column_name, :null_handling, 'NUMBER', :number_filter_type, :number_type_name, :min_value, :max_value)
                    """.trimIndent()

                    val params = mapOf(
                        "config_id" to configId,
                        "column_name" to columnName,
                        "null_handling" to filterData.nullHandling.value,
                        "number_filter_type" to filterData.filterType.value,
                        "number_type_name" to numberTypes[filterData.numberClass.simpleName!!],
                        "min_value" to filterData.minValue.value?.toString(),
                        "max_value" to filterData.maxValue.value?.toString()
                    )

                    updater.executeUpdate(filterSql, params)
                }

                is EnumFilterData<*> -> {
                    if (!filterData.isActive()) return@forEach
                    val enumValues = filterData.values

                    val filterSql = """
                        INSERT INTO public.report_filter_configs 
                        (report_config_id, column_name, null_handling, filter_type, enum_type_name, enum_values, include_enum)
                        VALUES (:config_id, :column_name, :null_handling, 'ENUM', :enum_type_name, :enum_values, :include_enum)
                    """.trimIndent()

                    val params = mapOf(
                        "config_id" to configId,
                        "column_name" to columnName,
                        "null_handling" to filterData.nullHandling.value,
                        "enum_type_name" to Converters.camelToSnakeCase(filterData.enumClass.simpleName!!),
                        "enum_values" to enumValues.toList(),
                        "include_enum" to filterData.include.value
                    )

                    updater.executeUpdate(filterSql, params)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseConfigurationFromNewRow(row: Map<String, Any?>): ReportConfiguration {
        val sortOrder = row["sort_order"] as List<SortConfiguration>
        val visibleColumns = row["visible_columns"] as List<String>
        val columnOrder = row["column_order"] as List<String>

        val configData = ReportConfigurationData(
            visibleColumns = visibleColumns,
            columnOrder = columnOrder,
            sortOrder = sortOrder,
            pageSize = row["page_size"] as Int
        )

        return ReportConfiguration(
            id = row["id"] as Int,
            name = row["name"] as String,
            reportName = row["report_name"] as String,
            description = row["description"] as String?,
            configuration = configData,
            isDefault = row["is_default"] as Boolean
        )
    }
}