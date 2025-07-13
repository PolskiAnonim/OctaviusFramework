package org.octavius.report.component

import org.octavius.database.DatabaseFetcher
import org.octavius.database.DatabaseManager
import org.octavius.domain.SortDirection
import org.octavius.report.Query
import org.octavius.report.ReportPaginationState
import org.octavius.report.filter.Filter
import org.octavius.report.filter.data.FilterData

class ReportDataManager(
    val reportStructure: ReportStructure
) {

    private fun <T : FilterData> getQueryFragment(
        columnName: String,
        filter: Filter<T>,
        data: FilterData
    ): Query? {
        @Suppress("UNCHECKED_CAST")
        val specificData = data as T

        return filter.createQueryFragment(columnName, specificData)
    }

    private fun buildFilterClause(reportState: ReportState, params: MutableMap<String, Any>): String {
        val columnFilters = reportState.filterData.mapNotNull { (columnKey, filterData) ->
            val column = reportStructure.getColumn(columnKey)!!
            getQueryFragment(columnKey, column.filter!!, filterData)
        }

        val filterConditions = columnFilters.map { fragment ->
            fragment.let {
                params.putAll(it.params)
                it.sql
            }
        }

        val searchConditions = buildSearchConditions(reportState, params)

        return combineConditions(filterConditions, searchConditions)
    }

    private fun buildSearchConditions(reportState: ReportState,params: MutableMap<String, Any>): List<String> {
        if (reportState.searchQuery.isBlank()) return emptyList()

        params["searchQuery"] = "%${reportState.searchQuery}%"

        return reportStructure.getAllColumns()
            .filter { (_, column) -> column.filterable }
            .map { (columnKey, _) -> "CAST($columnKey AS TEXT) ILIKE :searchQuery" }
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
        reportState: ReportState,
        fetcher: DatabaseFetcher,
        sourceSql: String,
        filterClause: String,
        params: MutableMap<String, Any>
    ): ReportPaginationState {
        val totalItems = fetcher.fetchCount(sourceSql, filterClause, params)
        val pageSize = reportState.pagination.pageSize
        val totalPages = if (totalItems == 0L) 0 else (totalItems + pageSize - 1) / pageSize

        return reportState.pagination.copy(totalItems = totalItems, totalPages = totalPages)
    }

    private fun buildOrderClause(reportState: ReportState): String {
        if (reportState.sortOrder.isEmpty()) return ""

        val sortDirectionMap = mapOf(
            SortDirection.Descending to "DESC",
            SortDirection.Ascending to "ASC"
        )

        val orderFragments = reportState.sortOrder.map { (columnKey, direction) ->
            "$columnKey ${sortDirectionMap[direction]}"
        }

        return orderFragments.joinToString(", ")
    }

    fun fetchData(reportState: ReportState): Pair<List<Map<String,Any?>>, ReportPaginationState> {

        val query = reportStructure.query
        val params = query.params.toMutableMap()
        val filterClause = buildFilterClause(reportState, params)
        val orderClause = buildOrderClause(reportState)

        val fetcher = DatabaseManager.getFetcher()

        // Najpierw zaktualizuj paginację
        val paginationState = updatePagination(reportState, fetcher, query.sql, filterClause, params)

        // Następnie pobierz dane dla aktualnej strony
        val offset = reportState.pagination.currentPage * reportState.pagination.pageSize
        val limit = reportState.pagination.pageSize

        val data = fetcher.fetchPagedList(
            table = query.sql,
            columns = "*",
            offset = offset.toInt(),
            limit = limit,
            filter = filterClause,
            orderBy = orderClause,
            params = params
        )

        return Pair(data, paginationState)
    }
}