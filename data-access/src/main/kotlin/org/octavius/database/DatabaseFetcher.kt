package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.QueryBuilder
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.reflect.KClass

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

    private val logger = KotlinLogging.logger {}

    private fun formatTableExpression(table: String): String {
        return if (table.trim().uppercase().contains(" ")) "($table)" else table
    }

    override fun select(columns: String, from: String): QueryBuilder {
        return DatabaseQueryBuilder(columns, from)
    }

    override fun fetchCount(table: String, filter: String?, params: Map<String, Any?>): Long {
        logger.debug { "Fetching count from: $from with filter: $filter" }
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT COUNT(*) AS count FROM ${formatTableExpression(table)}$whereClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
        
        logger.trace { "Executing count query: ${expanded.expandedSql} with params: ${expanded.expandedParams}" }
        val result = jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, Long::class.java) ?: 0L
        logger.debug { "Count result: $result" }
        return result
    }

    override fun fetchRowWithColumnInfo(tables: String, filter: String, params: Map<String, Any?>): Map<ColumnInfo, Any?>? {
        logger.debug { "Fetching row with column info from tables: $tables with filter: $filter" }
        val whereClause = " WHERE $filter"
        val sql = "SELECT * FROM ${formatTableExpression(tables)}$whereClause"
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        logger.trace { "Executing row query: ${expanded.expandedSql} with params: ${expanded.expandedParams}" }
        val results = jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnInfoMapper())
        
        return when (results.size) {
            0 -> {
                logger.debug { "No row found for filter: $filter" }
                null
            }
            1 -> {
                logger.debug { "Found single row for filter: $filter" }
                results[0]
            }
            else -> {
                logger.error { "Query returned ${results.size} rows for filter: $filter, expected 0 or 1" }
                throw IllegalStateException("Query returned ${results.size} rows, expected 0 or 1")
            }
        }
    }

    /**
     * Centralna, prywatna metoda do wykonywania wszystkich zapytań zbudowanych przez QueryBuilder.
     * Eliminuje powtarzanie kodu.
     */
    private fun <T : Any> executeQuery(builder: DatabaseQueryBuilder, params: Map<String, Any?>, rowMapper: RowMapper<T>): List<T> {
        val sqlBuilder = StringBuilder("SELECT ${builder.columns} FROM ${formatTableExpression(builder.table)}")
        builder.filter?.let { sqlBuilder.append(" WHERE $it") }
        builder.orderBy?.let { sqlBuilder.append(" ORDER BY $it") }
        builder.limit?.let { sqlBuilder.append(" LIMIT $it") }
        if (builder.offset > 0) sqlBuilder.append(" OFFSET ${builder.offset}")

        val sql = sqlBuilder.toString()
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
        
        logger.trace { "Executing query: ${expanded.expandedSql} with params: ${expanded.expandedParams}" }
        val results = jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMapper)
        logger.debug { "Query returned ${results.size} rows" }
        return results
    }

    /**
     * Prywatna, wewnętrzna klasa implementująca interfejs `QueryBuilder`.
     * Jako `inner class` ma dostęp do pól i metod zewnętrznej klasy `DatabaseFetcher`.
     */
    private inner class DatabaseQueryBuilder(
        val columns: String,
        val table: String
    ) : QueryBuilder {

        var filter: String? = null
        var orderBy: String? = null
        var limit: Long? = null
        var offset: Long = 0

        override fun where(condition: String?) = apply { this.filter = condition }
        override fun orderBy(ordering: String?) = apply { this.orderBy = ordering }
        override fun limit(count: Long?) = apply { this.limit = count }
        override fun offset(position: Long) = apply { this.offset = position }

        override fun page(page: Long, size: Long): QueryBuilder = apply {
            require(page >= 0) { "Numer strony (page) musi być większy lub równy 0." }
            require(size >= 1) { "Rozmiar strony (size) musi być większy lub równy 1." }

            val calculatedOffset = page * size
            this.limit(size)
            this.offset(calculatedOffset)
        }

        override fun toList(params: Map<String, Any?>): List<Map<String, Any?>> {
            return executeQuery(this, params, rowMappers.ColumnNameMapper())
        }

        override fun toSingle(params: Map<String, Any?>): Map<String, Any?>? {
            return limit(1).toList(params).firstOrNull()
        }

        override fun <T> toField(params: Map<String, Any?>): T? {
            val result = executeQuery(limit(1), params, rowMappers.SingleValueMapper())
            @Suppress("UNCHECKED_CAST")
            return result.firstOrNull() as T?
        }

        override fun <T> toColumn(params: Map<String, Any?>): List<T> {
            val result = executeQuery(this, params, rowMappers.SingleValueMapper())

            @Suppress("UNCHECKED_CAST")
            return result as List<T>
        }

        override fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): List<T> {
            return executeQuery(this, params, rowMappers.DataObjectMapper(kClass))
        }

        override fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): T? {
            val result = executeQuery(limit(1), params, rowMappers.DataObjectMapper(kClass))
            @Suppress("UNCHECKED_CAST")
            return result.firstOrNull() as T?
        }
    }
}