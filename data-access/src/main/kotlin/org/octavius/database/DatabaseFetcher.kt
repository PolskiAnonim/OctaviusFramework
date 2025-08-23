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
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    /**
     * Wykonuje zapytanie za pomocą executeQuery i opakowuje wynik lub błąd w DataResult.
     * Centralizuje logikę try-catch dla wszystkich operacji odczytu.
     */
    private fun <T> executeAndWrap(block: () -> T): DataResult<T> {
        return try {
            DataResult.Success(block())
        } catch (e: DatabaseException) {
            // Logujemy błąd na poziomie ERROR, ponieważ jest to obsłużony, ale istotny problem z bazą danych.
            logger.error(e) { "A known database exception occurred: $e" }
            DataResult.Failure(e)
        } catch (e: Exception) {
            // Logujemy błąd na poziomie ERROR, ponieważ jest to nieoczekiwany problem.
            val ex = QueryExecutionException("An unexpected error occurred before query execution.", "N/A", emptyMap(), e)
            logger.error(e) { "An unexpected error occurred during data fetching: $ex" }
            DataResult.Failure(ex)
        }
    }

    private fun formatTableExpression(table: String): String {
        return if (table.trim().uppercase().contains(" ")) "($table)" else table
    }

    override fun query(): QueryBuilder {
        return DatabaseQueryBuilder() // Tworzy pusty builder
    }

    override fun select(columns: String, from: String): QueryBuilder {
        return query().select(columns, from) // Używa nowego punktu wejścia
    }

    override fun fetchCount(from: String, filter: String?, params: Map<String, Any?>): DataResult<Long> {
        return executeAndWrap {
            val isSubquery = from.trim().uppercase().startsWith("SELECT")

            if (isSubquery) {
                logger.debug { "Fetching count from a subquery with filter: $filter" }
                logger.trace { "Subquery used for counting: $from" }
            } else {
                logger.debug { "Fetching count from table/view: '$from' with filter: $filter" }
            }

            val result = select("COUNT(*)", from)
                .where(filter)
                .toField<Long>(params)

            when (result) {
                is DataResult.Success -> {
                    val count = result.value ?: 0L
                    logger.debug { "Count successful. Result: $count" }
                    count
                }
                is DataResult.Failure -> throw result.error
            }
        }
    }

    override fun fetchRowWithColumnInfo(
        tables: String,
        filter: String,
        params: Map<String, Any?>
    ): DataResult<Map<ColumnInfo, Any?>?> {
        logger.debug { "Fetching single row with column info from '$tables' with filter: $filter" }
        return executeAndWrap {
            val results = executeQuery(
                query().select("*", tables).where(filter) as DatabaseQueryBuilder,
                params,
                rowMappers.ColumnInfoMapper()
            )

            when {
                results.size > 1 -> {
                    // To jest błąd programisty, logujemy jako WARN, bo wyjątek i tak przerwie operację.
                    logger.warn { "Query expected to return 0 or 1 row, but got ${results.size}. Filter: $filter" }
                    throw IllegalStateException("Query returned ${results.size} rows, expected 0 or 1")
                }
                else -> {
                    val row = results.firstOrNull()
                    logger.debug { if (row != null) "Row found." else "No row found." }
                    row
                }
            }
        }
    }

    /**
     * Centralna, prywatna metoda do wykonywania wszystkich zapytań zbudowanych przez QueryBuilder.
     * Eliminuje powtarzanie kodu.
     */
    private fun <T : Any?> executeQuery(
        builder: DatabaseQueryBuilder,
        params: Map<String, Any?>,
        rowMapper: RowMapper<T>
    ): List<T> {
        require(!builder.columns.isNullOrBlank() && !builder.table.isNullOrBlank()) {
            "Zapytanie jest niekompletne. Należy wywołać metodę .select(columns, from) na QueryBuilderze."
        }

        logger.debug { "Preparing to execute query from builder..." }

        val sqlBuilder = StringBuilder()

        if (builder.ctes.isNotEmpty()) {
            sqlBuilder.append("WITH ")
            if (builder.isRecursive) {
                sqlBuilder.append("RECURSIVE ")
            }
            // Łączymy wszystkie zdefiniowane CTE przecinkami
            val cteDefinitions = builder.ctes.joinToString(separator = ", \n") { (name, query) ->
                "$name AS ($query)"
            }
            sqlBuilder.append(cteDefinitions).append("\n")
        }
        sqlBuilder.append("SELECT ${builder.columns!!} FROM ${formatTableExpression(builder.table!!)}")
        builder.filter?.let { sqlBuilder.append(" WHERE $it") }
        builder.orderBy?.let { sqlBuilder.append(" ORDER BY $it") }
        builder.limit?.let { sqlBuilder.append(" LIMIT $it") }
        if (builder.offset > 0) sqlBuilder.append(" OFFSET ${builder.offset}")

        val sql = sqlBuilder.toString()
        val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)

        logger.trace { "Final SQL: ${expanded.expandedSql}" }
        logger.trace { "Final Params: ${expanded.expandedParams}" }

        logger.debug { "Executing query against the database." }
        val results = jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMapper)
        logger.debug { "Query executed successfully, returned ${results.size} rows." }
        return results
    }

    /**
     * Prywatna, wewnętrzna klasa implementująca interfejs `QueryBuilder`.
     * Jako `inner class` ma dostęp do pól i metod zewnętrznej klasy `DatabaseFetcher`.
     */
    private inner class DatabaseQueryBuilder : QueryBuilder {
        var columns: String? = null
        var table: String? = null

        val ctes = mutableListOf<Pair<String, String>>()
        var isRecursive: Boolean = false

        var filter: String? = null
        var orderBy: String? = null
        var limit: Long? = null
        var offset: Long = 0

        override fun with(name: String, query: String) = apply {
            ctes.add(name to query)
        }

        override fun recursive(recursive: Boolean) = apply {
            this.isRecursive = recursive
        }

        override fun select(columns: String, from: String) = apply {
            this.columns = columns
            this.table = from
        }

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

        override fun toList(params: Map<String, Any?>): DataResult<List<Map<String, Any?>>> {
            logger.debug { "Building query to fetch a list of maps." }
            return executeAndWrap { executeQuery(this, params, rowMappers.ColumnNameMapper()) }
        }

        override fun toSingle(params: Map<String, Any?>): DataResult<Map<String, Any?>?> {
            logger.debug { "Building query to fetch a single map." }
            this.limit(1)
            return executeAndWrap {
                executeQuery(this, params, rowMappers.ColumnNameMapper()).firstOrNull()
            }
        }

        override fun <T> toField(params: Map<String, Any?>): DataResult<T?> {
            logger.debug { "Building query to fetch a single field." }
            this.limit(1)
            return executeAndWrap {
                val result = executeQuery(this, params, rowMappers.SingleValueMapper()).firstOrNull()
                @Suppress("UNCHECKED_CAST")
                result as T?
            }
        }

        override fun <T> toColumn(params: Map<String, Any?>): DataResult<List<T?>> {
            logger.debug { "Building query to fetch a single column." }
            return executeAndWrap {
                @Suppress("UNCHECKED_CAST")
                executeQuery(this, params, rowMappers.SingleValueMapper()) as List<T?>
            }
        }

        override fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): DataResult<List<T>> {
            logger.debug { "Building query to fetch a list of ${kClass.simpleName} objects." }
            return executeAndWrap { executeQuery(this, params, rowMappers.DataObjectMapper(kClass)) }
        }

        override fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): DataResult<T?> {
            logger.debug { "Building query to fetch a single ${kClass.simpleName} object." }
            this.limit(1)
            return executeAndWrap {
                executeQuery(this, params, rowMappers.DataObjectMapper(kClass)).firstOrNull()
            }
        }
    }
}