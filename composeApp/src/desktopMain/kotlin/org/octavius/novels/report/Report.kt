package org.octavius.novels.report

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.report.column.ReportColumn

abstract class Report {

    private val query: Query
    private val columns: Map<String, ReportColumn>
    private val columnStates = mutableMapOf<String, ColumnState>()

    init {
        columns = this.createColumns()
        initializeColumnStates(columns)
        query = this.createQuery()
    }

    private val reportState = ReportState()

    private fun initializeColumnStates(columns: Map<String, ReportColumn>) {
        for ((key, column) in columns) {
            columnStates[key] = column.initializeState()
        }
    }

    open var onRowClick: ((Map<String, Any?>) -> Unit)? = null

    abstract fun createQuery(): Query

    abstract fun createColumns(): Map<String, ReportColumn>

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
            columns.forEach { (key, column) ->
                // Pomijamy kolumny, które są ukryte lub nie są typu tekstowego
                if (columnStates[key]?.visible?.value == true) {
                    searchConditions.add("CAST(${column.fieldName} AS TEXT) ILIKE '%$searchQuery%'")
                }
            }

            // Łączymy wszystkie warunki operatorem OR
            if (searchConditions.isNotEmpty()) {
                whereClauseBuilder.append("(${searchConditions.joinToString(" OR ")})")
            }
        }

        // Dodaj warunki filtrowania z columnStates
        for ((key, state) in columnStates) {
            val filter = state.filtering.value ?: continue
            if (!filter.isActive()) continue

            val column = columns[key]!!
            val whereClause = column.filter!!.constructWhereClause(filter)

            if (whereClause.isNotEmpty()) {
                if (whereClauseBuilder.isNotEmpty()) {
                    whereClauseBuilder.append(" AND ")
                }
                whereClauseBuilder.append(whereClause)
            }
        }

        val fetcher = DatabaseManager.getFetcher()
        try {

            val totalCount = fetcher.fetchCount(query.sql, whereClauseBuilder.toString()) / pageSize
            val results = fetcher.fetchPagedList(
                table = query.sql,
                fields = "*",
                offset = page * pageSize,
                limit = pageSize,
                filter = whereClauseBuilder.toString())

            onResult(results, totalCount)
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(emptyList(), 0)
        }
    }

    fun areAnyFiltersActive(): Boolean {
        return columnStates.values.any { state ->
            state.filtering.value?.isActive() == true
        }
    }

    fun getColumns(): Map<String, ReportColumn> = columns
    
    fun getColumnStates(): Map<String, ColumnState> = columnStates

}