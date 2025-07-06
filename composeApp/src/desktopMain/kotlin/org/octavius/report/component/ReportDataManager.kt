package org.octavius.report.component

import kotlinx.coroutines.runBlocking
import org.octavius.database.DatabaseFetcher
import org.octavius.database.DatabaseManager
import org.octavius.domain.SortDirection

class ReportDataManager {

    lateinit var reportStructure: ReportStructure
    lateinit var reportState: ReportState

    fun setReferences(reportStructure: ReportStructure, reportState: ReportState) {
        this.reportStructure = reportStructure
        this.reportState = reportState
    }

    private fun buildFilterClause(params: MutableMap<String, Any>): String {
        val columnFilters = reportState.filterData.value.mapNotNull { (columnKey, filterData) ->
            val column = reportStructure.getColumn(columnKey)
            column?.let { filterData.getFilterFragment(it.databaseColumnName) }
        }

        val filterConditions = columnFilters.map { fragment ->
            fragment.let {
                params.putAll(it.params)
                it.sql
            }
        }

        val searchConditions = buildSearchConditions(params)

        return combineConditions(filterConditions, searchConditions)
    }

    private fun buildSearchConditions(params: MutableMap<String, Any>): List<String> {
        if (reportState.searchQuery.value.isBlank()) return emptyList()

        params["searchQuery"] = "%${reportState.searchQuery.value}%"

        return reportStructure.getAllColumns()
            .filter { (_, column) -> column.filterable }
            .map { (_, column) -> "CAST(${column.databaseColumnName} AS TEXT) ILIKE :searchQuery" }
    }

    private fun combineConditions(filterConditions: List<String>, searchConditions: List<String>): String {
        val parts = mutableListOf<String>()

        if (filterConditions.isNotEmpty()) {
            parts.add(filterConditions.joinToString(" AND "))
        }

        if (searchConditions.isNotEmpty()) {
            parts.add("(${searchConditions.joinToString(" OR ")})")
        }

        return parts.joinToString(" AND ")
    }

    private fun updatePagination(
        fetcher: DatabaseFetcher,
        sourceSql: String,
        filterClause: String,
        params: MutableMap<String, Any>
    ) {
        val totalItems = fetcher.fetchCount(sourceSql, filterClause, params)
        val pageSize = reportState.pagination.pageSize.value
        val totalPages = if (totalItems == 0L) 0 else (totalItems + pageSize - 1) / pageSize

        reportState.pagination.totalItems.value = totalItems
        reportState.pagination.totalPages.value = totalPages
    }

    private fun buildOrderClause(): String {
        if (reportState.sortOrder.value.isEmpty()) return ""

        val sortDirectionMap = mapOf(
            SortDirection.Descending to "DESC",
            SortDirection.Ascending to "ASC"
        )

        val orderFragments = reportState.sortOrder.value.mapNotNull { (columnKey, direction) ->
            val column = reportStructure.getColumn(columnKey)
            column?.let { "${it.databaseColumnName} ${sortDirectionMap[direction]}" }
        }

        return orderFragments.joinToString(", ")
    }

    fun fetchData() {
        reportState.setLoading(true)
        reportState.setError(null)

        val query = reportStructure.query
        val params = query.params.toMutableMap()
        val filterClause = buildFilterClause(params)
        val orderClause = buildOrderClause()

        val fetcher = DatabaseManager.getFetcher()

        // Najpierw zaktualizuj paginację
        updatePagination(fetcher, query.sql, filterClause, params)

        // Następnie pobierz dane dla aktualnej strony
        val offset = reportState.pagination.currentPage.value * reportState.pagination.pageSize.value
        val limit = reportState.pagination.pageSize.value

        val data = fetcher.fetchPagedList(
            table = query.sql,
            columns = "*",
            offset = offset.toInt(),
            limit = limit,
            filter = filterClause,
            orderBy = orderClause,
            params = params
        )

        reportState.data = data
        reportState.setLoading(false)
    }

    fun refreshData() {
        // Wrapper dla compatibility z synchronicznym kodem
        runBlocking {
            fetchData()
        }
    }

}