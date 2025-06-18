package org.octavius.report

import org.octavius.database.DatabaseManager
import org.octavius.report.column.ReportColumn
import org.octavius.report.components.ReportStructure
import org.octavius.report.filter.Filter

abstract class Report {

    private val reportStructure: ReportStructure
    private val reportState = ReportState()
    private val filters: Map<String, Filter>

    init {
        reportStructure = this.createReportStructure()
        filters = initializeFilters()
        reportState.initialize(reportStructure.getAllColumns().keys, reportStructure.reportConfig)
    }

    private fun initializeFilters(): Map<String, Filter> {
        val filterMap = mutableMapOf<String, Filter>()
        val filterValues = mutableMapOf<String, FilterData<*>>()
        
        reportStructure.getAllColumns().forEach { (columnName, column) ->
            if (column.filterable) {
                val filter = column.createFilter()
                if (filter != null) {
                    filterMap[columnName] = filter
                    // Inicjalizuj domyślną wartość filtra
                    filterValues[columnName] = filter.createFilterData()
                }
            }
        }
        
        // Ustaw początkowe wartości filtrów w reportState
        reportState.filterValues.value = filterValues
        
        return filterMap
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
                    searchConditions.add("CAST(${column.fieldName} AS TEXT) ILIKE '%$searchQuery%'")
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

            val filter = filters[columnName] ?: continue
            val whereClause = filter.constructWhereClause(filterData)

            if (whereClause.isNotEmpty()) {
                if (whereClauseBuilder.isNotEmpty()) {
                    whereClauseBuilder.append(" AND ")
                }
                whereClauseBuilder.append(whereClause)
            }
        }

        // Budowanie klauzuli ORDER BY dla sortowania
        val orderByClause = StringBuilder()
        if (reportState.sortOrder.value.isNotEmpty()) {
            val sortConditions = reportState.sortOrder.value.mapNotNull { (columnName, direction) ->
                val column = reportStructure.getAllColumns()[columnName]
                if (column != null) {
                    val directionStr = when (direction) {
                        SortDirection.ASC -> "ASC"
                        SortDirection.DESC -> "DESC"
                    }
                    "${column.fieldName} $directionStr"
                } else null
            }

            if (sortConditions.isNotEmpty()) {
                orderByClause.append(sortConditions.joinToString(", "))
            }
        }

        val fetcher = DatabaseManager.getFetcher()
        try {
            val totalCount = fetcher.fetchCount(reportStructure.query.sql, whereClauseBuilder.toString()) / pageSize
            val results = fetcher.fetchPagedList(
                table = reportStructure.query.sql,
                fields = "*",
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
    
    fun getFilters(): Map<String, Filter> = filters

}