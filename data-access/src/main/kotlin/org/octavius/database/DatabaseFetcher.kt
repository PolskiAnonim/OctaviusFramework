package org.octavius.database

import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Implementacja `DataFetcher` do pobierania danych z bazy PostgreSQL.
 *
 * Zapewnia metody do wykonywania zapytań SELECT, automatycznie obsługując
 * filtrowanie, paginację oraz **ekspansję złożonych parametrów** (np. `List` -> `ARRAY`,
 * `data class` -> `ROW`).
 *
 * @param jdbcTemplate Template Spring JDBC do wykonywania zapytań.
 * @param rowMappers Fabryka mapperów do konwersji wyników na obiekty Kotlin.
 * @param kotlinToPostgresConverter Helper do ekspansji złożonych parametrów (list, enumów,
 *   data class) na składnię SQL.
 */
class DatabaseFetcher(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
) : DataFetcher {

    /** Formatuje wyrażenie tabelowe, dodając nawiasy dla zapytań z JOIN lub subquery. */
    private fun formatTableExpression(table: String): String {
        return if (table.trim().uppercase().contains(" ")) "($table)" else table
    }

    /** Pobiera liczbę wierszy spełniających podane kryteria. */
    override fun fetchCount(table: String, filter: String?, params: Map<String, Any?>): Long {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT COUNT(*) AS count FROM ${formatTableExpression(table)}$whereClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, Long::class.java) ?: 0L
    }


    /** Pobiera wartość pojedynczego pola z pierwszego pasującego wiersza. */
    override fun fetchField(table: String, field: String, filter: String?, params: Map<String, Any?>): Any? {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT $field FROM ${formatTableExpression(table)}$whereClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        val result = jdbcTemplate.query(
            expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper()
        )
        // Pierwsze pole lub null
        return result.firstOrNull()
    }

    /**
     * Pobiera pojedynczy wiersz jako mapę.
     * @throws NullPointerException gdy nie znaleziono wiersza.
     * @throws IllegalStateException gdy zapytanie zwróci więcej niż 1 wiersz.
     */
    override fun fetchRow(
        table: String,
        columns: String,
        filter: String?,
        params: Map<String, Any?>
    ): Map<String, Any?> {
        val results = fetchRowOrNull(table, columns, filter, params)
        return results!!
    }

    /**
     * Pobiera pojedynczy wiersz jako mapę lub null, jeśli nie znaleziono.
     * @throws IllegalStateException gdy zapytanie zwróci więcej niż 1 wiersz.
     */
    override fun fetchRowOrNull(
        table: String, columns: String, filter: String?, params: Map<String, Any?>
    ): Map<String, Any?>? {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT $columns FROM ${formatTableExpression(table)}$whereClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
        val results = jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())

        return when (results.size) {
            0 -> null
            1 -> results[0]
            else -> throw IllegalStateException("Query returned ${results.size} rows, expected 0 or 1")
        }
    }

    /** Pobiera listę wartości z pojedynczej kolumny. */
    override fun fetchColumn(
        table: String,
        column: String,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $column FROM ${formatTableExpression(table)}$whereClause$orderClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    /** Pobiera paginowaną listę wartości z pojedynczej kolumny. */
    override fun fetchPagedColumn(
        table: String,
        column: String,
        offset: Int,
        limit: Int,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql =
            "SELECT $column FROM ${formatTableExpression(table)}$whereClause$orderClause LIMIT $limit OFFSET $offset"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    /** Pobiera listę wierszy jako listę map. */
    override fun fetchList(
        table: String,
        columns: String,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Map<String, Any?>> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $columns FROM ${formatTableExpression(table)}$whereClause$orderClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())
    }

    /** Pobiera paginowaną listę wierszy. */
    override fun fetchPagedList(
        table: String,
        columns: String,
        offset: Int,
        limit: Int,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Map<String, Any?>> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql =
            "SELECT $columns FROM ${formatTableExpression(table)}$whereClause$orderClause LIMIT $limit OFFSET $offset"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())
    }

    /**
     * Pobiera pojedynczy wiersz, mapując go na obiekty `ColumnInfo`.
     *
     * Używane do pobierania danych do formularzy, gdzie ważne jest pochodzenie kolumny (nazwa tabeli).
     * Wymaga filtra, który zapewni zwrot tylko jednego wiersza.
     *
     * @param tables Wyrażenie tabelowe, np. "users u JOIN profiles p ON u.id = p.user_id".
     * @return Mapa `ColumnInfo` do wartości.
     */
    override fun fetchEntity(
        tables: String,
        filter: String?,
        params: Map<String, Any?>
    ): Map<ColumnInfo, Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT * FROM ${formatTableExpression(tables)}$whereClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnInfoMapper())
            ?: emptyMap()
    }
}