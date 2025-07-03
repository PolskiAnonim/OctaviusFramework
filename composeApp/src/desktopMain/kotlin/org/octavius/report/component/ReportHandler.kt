package org.octavius.report.component

import org.octavius.database.DatabaseManager
import org.octavius.report.filter.data.FilterData
import org.octavius.domain.SortDirection
import org.octavius.report.column.ReportColumn
import org.octavius.report.ReportConfigurationManager

abstract class ReportHandler {

    private val reportStructure: ReportStructure
    private val reportState = ReportState()

    init {
        reportStructure = this.createReportStructure()
        val filterValues = reportStructure.getAllColumns().filterValues { v -> v.filterable }.mapValues { it.value.getFilterData()!! }
        reportState.filterValues.value = filterValues
        reportState.initialize(reportStructure.getAllColumns().keys, reportStructure.reportConfig)
        loadDefaultConfiguration()
    }

    open var onRowClick: ((Map<String, Any?>) -> Unit)? = null

    abstract fun createReportStructure(): ReportStructure

    fun fetchData(
        page: Int,
        searchQuery: String,
        pageSize: Int,
        onResult: (List<Map<String, Any?>>, Long) -> Unit
    ) {
        // Budowanie klauzuli WHERE dla wyszukiwania
        val whereClauseBuilder = StringBuilder()

        // Dodaj warunek wyszukiwania dla searchQuery jeśli nie jest pusty
        if (searchQuery.isNotEmpty()) {
            val searchConditions = mutableListOf<String>()

            // Dla każdej widocznej kolumny dodaj warunek wyszukiwania
            reportStructure.getAllColumns().forEach { (key, column) ->
                // Pomijamy kolumny, które są ukryte
                if (reportState.visibleColumns.value.contains(key)) {
                    searchConditions.add("CAST(${column.databaseColumnName} AS TEXT) ILIKE '%$searchQuery%'")
                }
            }

            // Łączymy wszystkie warunki operatorem OR
            if (searchConditions.isNotEmpty()) {
                whereClauseBuilder.append("(${searchConditions.joinToString(" OR ")})")
            }
        }

        // Dodaj warunki filtrowania z reportState
        for ((columnName, filterData) in reportState.filterValues.value) {
            if (!filterData.isActive()) continue

            val filterFragment = filterData.getFilterFragment(columnName)
            
            if (filterFragment != null) {
                if (whereClauseBuilder.isNotEmpty()) {
                    whereClauseBuilder.append(" AND ")
                }
                whereClauseBuilder.append(filterFragment.sql)
            }
        }

        // Budowanie klauzuli ORDER BY dla sortowania
        val orderByClause = StringBuilder()
        if (reportState.sortOrder.value.isNotEmpty()) {
            val sortConditions = reportState.sortOrder.value.mapNotNull { (columnName, direction) ->
                val column = reportStructure.getAllColumns()[columnName]
                if (column != null) {
                    val directionStr = when (direction) {
                        SortDirection.Ascending -> "ASC"
                        SortDirection.Descending -> "DESC"
                    }
                    "${column.databaseColumnName} $directionStr"
                } else null
            }

            if (sortConditions.isNotEmpty()) {
                orderByClause.append(sortConditions.joinToString(", "))
            }
        }

        val fetcher = DatabaseManager.getFetcher()
        try {
            val totalCount = (fetcher.fetchCount(reportStructure.query.sql, whereClauseBuilder.toString()) + pageSize - 1) / pageSize
            val results = fetcher.fetchPagedList(
                table = reportStructure.query.sql,
                columns = "*",
                offset = page * pageSize,
                limit = pageSize,
                filter = whereClauseBuilder.toString(),
                orderBy = orderByClause.toString())

            onResult(results, totalCount)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(emptyList(), 0)
        }
    }

    fun areAnyFiltersActive(): Boolean {
        return reportState.filterValues.value.values.any { filterData ->
            filterData.isActive()
        }
    }

    fun getColumns(): Map<String, ReportColumn> = reportStructure.getAllColumns()

    fun getReportState(): ReportState = reportState

    private fun loadDefaultConfiguration() {
        try {
            val configManager = ReportConfigurationManager()
            val defaultConfig = configManager.loadDefaultConfiguration(reportStructure.reportName)
            
            if (defaultConfig != null) {
                configManager.applyConfiguration(defaultConfig, reportState)
            }
        } catch (e: Exception) {
            // Jeśli nie ma domyślnej konfiguracji lub wystąpił błąd, ignorujemy to
            println("Nie udało się załadować domyślnej konfiguracji: ${e.message}")
        }
    }

    fun getReportName(): String {
        return reportStructure.reportName
    }

}