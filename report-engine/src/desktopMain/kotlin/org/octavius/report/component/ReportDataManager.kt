package org.octavius.report.component

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DataResult
import org.octavius.domain.SortDirection
import org.octavius.report.Query
import org.octavius.report.ReportDataResult
import org.octavius.report.ReportPaginationState
import org.octavius.report.filter.Filter
import org.octavius.report.filter.data.FilterData

class ReportDataManager(
    val reportStructure: ReportStructure
) : KoinComponent {
    val fetcher: DataFetcher by inject()
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
            val column = reportStructure.getColumn(columnKey)
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

    private fun buildSearchConditions(reportState: ReportState, params: MutableMap<String, Any>): List<String> {
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
        sourceSql: String,
        filterClause: String,
        params: Map<String, Any>
    ): DataResult<ReportPaginationState> {

        val countResult = fetcher.query().from(sourceSql).where(filterClause).toCount(params)

        return when (countResult) {
            is DataResult.Success -> {
                val totalItems = countResult.value
                val pageSize = reportState.pagination.pageSize
                val totalPages = if (totalItems == 0L) 0 else (totalItems + pageSize - 1) / pageSize
                val newPaginationState = reportState.pagination.copy(totalItems = totalItems, totalPages = totalPages)
                DataResult.Success(newPaginationState)
            }

            is DataResult.Failure -> {
                // Po prostu propagujemy błąd dalej
                countResult
            }
        }
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

    fun fetchData(reportState: ReportState): ReportDataResult {

        val query = reportStructure.query
        val params = query.params.toMutableMap()
        val filterClause = buildFilterClause(reportState, params)
        val orderClause = buildOrderClause(reportState)

        val paginationResult = updatePagination(reportState, query.sql, filterClause, params)

        val newPaginationState = when (paginationResult) {
            is DataResult.Success -> paginationResult.value
            is DataResult.Failure -> {
                return ReportDataResult.Failure(paginationResult.error)
            }
        }

        if (newPaginationState.totalItems == 0L) {
            return ReportDataResult.Success(emptyList(), newPaginationState)
        }

        val dataResult = fetcher.select(from = query.sql)
            .where(filterClause.takeIf { it.isNotBlank() })
            .orderBy(orderClause.takeIf { it.isNotBlank() })
            .page(reportState.pagination.currentPage, reportState.pagination.pageSize)
            .toList(params)

        return when (dataResult) {
            is DataResult.Success -> {
                ReportDataResult.Success(dataResult.value, newPaginationState)
            }

            is DataResult.Failure -> {
                ReportDataResult.Failure(dataResult.error)
            }
        }
    }
}