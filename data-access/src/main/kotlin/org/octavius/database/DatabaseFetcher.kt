package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DataResult
import org.octavius.data.contract.QueryBuilder
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.exception.DatabaseException
import org.octavius.exception.QueryExecutionException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.reflect.KClass

/**
 * Implementacja [DataFetcher] wykorzystująca [NamedParameterJdbcTemplate]
 * do interakcji z bazą danych i [RowMappers] do mapowania wyników.
 */
class DatabaseFetcher(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val rowMappers: RowMappers,
    val kotlinToPostgresConverter: KotlinToPostgresConverter
) : DataFetcher {
    override fun query(): QueryBuilder {
        return DatabaseQueryBuilder(jdbcTemplate, rowMappers, kotlinToPostgresConverter)
    }

    override fun select(columns: String, from: String): QueryBuilder {
        return query().select(columns).from(from)
    }
}

/**
 * Wewnętrzna implementacja [QueryBuilder].
 * Zarządza stanem budowanego zapytania i generuje SQL.
 */
internal class DatabaseQueryBuilder(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
) : QueryBuilder {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private var recursiveWith: Boolean = false
    private val withClauses: MutableList<Pair<String, String>> = mutableListOf()
    private var selectClause: String? = null
    private var fromClause: String? = null
    private var whereCondition: String? = null
    private var orderByClause: String? = null
    private var limitValue: Long? = null
    private var offsetValue: Long? = null

    override fun with(name: String, query: String): QueryBuilder = apply {
        withClauses.add(name to query)
    }

    override fun recursive(recursive: Boolean): QueryBuilder = apply {
        this.recursiveWith = recursive
    }

    override fun select(columns: String): QueryBuilder = apply {
        this.selectClause = columns
    }

    override fun from(query: String): QueryBuilder = apply {
        this.fromClause = query
    }

    override fun where(condition: String?): QueryBuilder = apply {
        this.whereCondition = condition
    }

    override fun orderBy(ordering: String?): QueryBuilder = apply {
        this.orderByClause = ordering
    }

    override fun limit(count: Long?): QueryBuilder = apply {
        this.limitValue = count
    }

    override fun offset(position: Long): QueryBuilder = apply {
        this.offsetValue = position
    }

    override fun page(page: Long, size: Long): QueryBuilder = apply {
        require(page >= 0) { "Page number cannot be negative." }
        require(size > 0) { "Page size must be positive." }
        this.offsetValue = page * size
        this.limitValue = size
    }

    // --- Metody Terminalne ---

    override fun toList(params: Map<String, Any?>): DataResult<List<Map<String, Any?>>> {
        val sql = buildSql(defaultSelect = "*")
        return execute(sql, params, rowMappers.ColumnNameMapper()) { DataResult.Success(it) }
    }

    override fun toSingle(params: Map<String, Any?>): DataResult<Map<String, Any?>?> {
        val sql = buildSql(defaultSelect = "*", forceLimitOne = true)
        return execute(sql, params, rowMappers.ColumnNameMapper()) { DataResult.Success(it.firstOrNull()) }
    }

    override fun <T> toField(params: Map<String, Any?>): DataResult<T?> {
        val sql = buildSql(defaultSelect = "*", forceLimitOne = true)
        return execute(sql, params, rowMappers.SingleValueMapper()) {
            @Suppress("UNCHECKED_CAST")
            DataResult.Success(it.firstOrNull() as T?)
        }
    }

    override fun <T> toColumn(params: Map<String, Any?>): DataResult<List<T?>> {
        val sql = buildSql(defaultSelect = "*")
        return execute(sql, params, rowMappers.SingleValueMapper()) {
            @Suppress("UNCHECKED_CAST")
            DataResult.Success(it as List<T?>)
        }
    }

    override fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): DataResult<List<T>> {
        val sql = buildSql(defaultSelect = "*")
        return execute(sql, params, rowMappers.DataObjectMapper(kClass)) { DataResult.Success(it) }
    }

    override fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): DataResult<T?> {
        val sql = buildSql(defaultSelect = "*", forceLimitOne = true)
        return execute(sql, params, rowMappers.DataObjectMapper(kClass)) { DataResult.Success(it.firstOrNull()) }
    }

    override fun toSingleWithColumnInfo(params: Map<String, Any?>): DataResult<Map<ColumnInfo, Any?>?> {
        val sql = buildSql(defaultSelect = "*", forceLimitOne = true)
        return execute(sql, params, rowMappers.ColumnInfoMapper()) { DataResult.Success(it.firstOrNull()) }
    }

    override fun toCount(params: Map<String, Any?>): DataResult<Long> {
        val sql = buildSql(defaultSelect = "COUNT(*)", forceLimitOne = true)
        return execute(sql, params, rowMappers.SingleValueMapper()) {
            DataResult.Success(it.firstOrNull() as? Long ?: 0L)
        }
    }

    /**
     * Formatuje wyrażenie tabeli na potrzeby klauzuli FROM.
     * Opakowuje w nawiasy złożone wyrażenia (podzapytania, złączenia),
     * a pozostawia bez zmian proste tabele (np. "users" lub "users u").
     *
     * @param table Wyrażenie tabeli, np. "users", "users u", "table_a JOIN table_b ON ...", "SELECT ...".
     * @return Sformatowane wyrażenie tabeli.
     */
    private fun formatTableExpression(table: String): String {
        val trimmed = table.trim()

        val upper = trimmed.uppercase()

        if (upper.startsWith("(") && upper.endsWith(")")) {
            return trimmed
        }

        if (upper.contains(Regex("\\bSELECT\\b")) || upper.contains(Regex("\\bSELECT\\b"))) {
            return "($trimmed)"
        }

        return trimmed
    }

    /**
     * Składa ostateczny SQL na podstawie skonfigurowanego stanu.
     * @param defaultSelect Domyślna klauzula SELECT, jeśli nie została jawnie ustawiona.
     * @param forceLimitOne Jeśli true, dodaje LIMIT 1 do zapytania, nadpisując istniejący limit.
     */
    private fun buildSql(defaultSelect: String, forceLimitOne: Boolean = false): String {
        if (fromClause == null) {
            throw IllegalStateException("FROM clause must be defined before executing a query.")
        }

        val sqlBuilder = StringBuilder()

        // 1. WITH clause
        if (withClauses.isNotEmpty()) {
            sqlBuilder.append("WITH ")
            if (recursiveWith) {
                sqlBuilder.append("RECURSIVE ")
            }
            sqlBuilder.append(withClauses.joinToString(", ") { "${it.first} AS (${it.second})" })
            sqlBuilder.append(" ")
        }

        // 2. SELECT clause
        sqlBuilder.append("SELECT ").append(selectClause ?: defaultSelect).append(" ")

        // 3. FROM clause
        sqlBuilder.append("FROM ").append(formatTableExpression(fromClause!!)).append(" ")

        // 4. WHERE clause
        whereCondition?.takeIf { it.isNotBlank() }?.let {
            sqlBuilder.append("WHERE ").append(it).append(" ")
        }

        // 5. ORDER BY clause
        orderByClause?.takeIf { it.isNotBlank() }?.let {
            sqlBuilder.append("ORDER BY ").append(it).append(" ")
        }

        // 6. LIMIT clause (handle forceLimitOne)
        val effectiveLimit = if (forceLimitOne) 1L else limitValue
        effectiveLimit?.takeIf { it > 0 }?.let {
            sqlBuilder.append("LIMIT ").append(it).append(" ")
        }

        // 7. OFFSET clause
        offsetValue?.takeIf { it >= 0 }?.let {
            sqlBuilder.append("OFFSET ").append(it).append(" ")
        }

        val finalSql = sqlBuilder.toString().trim()
        logger.debug { "Built SQL: $finalSql" }
        return finalSql
    }

    /**
     * Generyczna funkcja pomocnicza do wykonywania zapytań SQL i mapowania wyników.
     * Minimalizuje duplikację kodu i centralizuje obsługę błędów.
     *
     * @param sql Zapytanie SQL do wykonania.
     * @param params Mapa nazwanych parametrów.
     * @param rowMapper [RowMapper] używany do mapowania pojedynczego wiersza [ResultSet] na typ `M`.
     * @param transform Funkcja transformująca listę zmapowanych obiektów (`List<M>`)
     *                  na ostateczny [DataResult] typu `R`.
     * @return [DataResult] zawierający przetransformowane wyniki lub błąd.
     */
    private fun <R, M> execute(
        sql: String,
        params: Map<String, Any?>,
        rowMapper: RowMapper<M>,
        transform: (List<M>) -> DataResult<R>
    ): DataResult<R> {
        var expandedSql: String? = null
        var expandedParams: Map<String, Any?>? = null
        return try {
            val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
            expandedSql = expanded.expandedSql
            expandedParams = expanded.expandedParams
            logger.debug { "Executing query (expanded): $expandedSql with params: $expandedParams" }
            val results: List<M> = jdbcTemplate.query(expandedSql, expandedParams, rowMapper)
            transform(results)
        } catch (e: DatabaseException) {
            logger.error(e) { "Database error executing query: $expandedSql with params: $expandedParams" }
            DataResult.Failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error executing query: $expandedSql with params: $expandedParams" }
            DataResult.Failure(
                QueryExecutionException(
                    "Unexpected error during query execution.", sql = expandedSql ?: "SQL not generated",
                    params = expandedParams ?: emptyMap(), e
                )
            )
        }
    }
}