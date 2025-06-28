package org.octavius.database

import org.octavius.form.ColumnInfo
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class DatabaseFetcher(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val rowMappers: RowMappers
) {
    private val parameterExpandHelper = ParameterExpandHelper()
    
    private fun formatTableExpression(table: String): String {
        return if (table.trim().uppercase().contains(" ")) "($table)" else table
    }

    fun fetchCount(table: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Long {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT COUNT(*) AS count FROM ${formatTableExpression(table)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, Long::class.java) ?: 0L
    }


    fun fetchField(table: String, field: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Any? {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT $field FROM ${formatTableExpression(table)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    fun fetchRow(
        table: String,
        fields: String,
        filter: String? = null,
        params: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        val results = fetchRowOrNull(table, fields, filter, params)
        return results!!
    }

    fun fetchRowOrNull(
        table: String,
        fields: String,
        filter: String? = null,
        params: Map<String, Any?> = emptyMap()
    ): Map<String, Any?>? {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT $fields FROM ${formatTableExpression(table)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)
        val results = jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())

        return results
    }

    fun fetchColumn(
        table: String,
        column: String,
        filter: String? = null,
        orderBy: String? = null,
        params: Map<String, Any?> = emptyMap()
    ): List<Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $column FROM ${formatTableExpression(table)}$whereClause$orderClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    fun fetchPagedColumn(
        table: String,
        column: String,
        offset: Int,
        limit: Int,
        filter: String? = null,
        orderBy: String? = null,
        params: Map<String, Any?> = emptyMap()
    ): List<Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $column FROM ${formatTableExpression(table)}$whereClause$orderClause LIMIT $limit OFFSET $offset"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    fun fetchList(
        table: String,
        fields: String,
        filter: String? = null,
        orderBy: String? = null,
        params: Map<String, Any?> = emptyMap()
    ): List<Map<String, Any?>> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $fields FROM ${formatTableExpression(table)}$whereClause$orderClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())
    }

    fun fetchPagedList(
        table: String,
        fields: String,
        offset: Int,
        limit: Int,
        filter: String? = null,
        orderBy: String? = null,
        params: Map<String, Any?> = emptyMap()
    ): List<Map<String, Any?>> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $fields FROM ${formatTableExpression(table)}$whereClause$orderClause LIMIT $limit OFFSET $offset"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())
    }

    fun fetchEntity(
        tables: String,
        filter: String? = null,
        params: Map<String, Any?> = emptyMap()
    ) : Map<ColumnInfo, Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT * FROM ${formatTableExpression(tables)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnInfoMapper()) ?: emptyMap()
    }
}