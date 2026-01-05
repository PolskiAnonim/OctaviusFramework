package org.octavius.database.builder

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataResult
import org.octavius.data.builder.StreamingTerminalMethods
import org.octavius.data.exception.QueryExecutionException
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import kotlin.reflect.KClass

internal class StreamingQueryBuilder(
    private val builder: AbstractQueryBuilder<*>,
    private val fetchSize: Int
) : StreamingTerminalMethods {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun <T> executeStream(
        params: Map<String, Any?>,
        rowMapper: RowMapper<T>,
        action: (item: T) -> Unit
    ): DataResult<Unit> {
        // Deklarujemy zmienne na zewnątrz, aby były dostępne w `catch`
        val originalSql = builder.buildSql()
        var expandedSql: String? = null
        var expandedParams: Map<String, Any?>? = null

        return try {
            val expanded = builder.kotlinToPostgresConverter.expandParametersInQuery(originalSql, params)
            expandedSql = expanded.expandedSql
            expandedParams = expanded.expandedParams

            logger.debug { "Executing streaming query (expanded): $expandedSql with params: $expandedParams" }

            builder.jdbcTemplate.execute(expandedSql, expandedParams) { ps ->
                // --- WARN-FAST MECHANISM ---
                if (ps.connection.autoCommit) {
                    logger.warn {
                        "POTENTIAL PERFORMANCE ISSUE: Streaming query executed with autoCommit=true. " +
                                "PostgreSQL driver will ignore fetchSize=$fetchSize and load all rows into RAM. " +
                                "Wrap this call in DataAccess.transaction { ... }."
                    }
                }

                ps.fetchSize = this.fetchSize

                val rs: ResultSet = ps.executeQuery()
                var rowNum = 0
                while (rs.next()) {
                    val mappedItem = rowMapper.mapRow(rs, rowNum++)
                    action(mappedItem) // Wywołujemy akcję użytkownika dla każdego wiersza
                }
            }

            DataResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Database error executing streaming query: $expandedSql" }
            DataResult.Failure(
                QueryExecutionException(
                    sql = expandedSql ?: originalSql,
                    params = expandedParams ?: params,
                    message = "Streaming query failed.",
                    cause = e
                )
            )
        }
    }

    // --- Publiczne metody terminalne, które używają metody pomocniczej ---

    override fun forEachRow(params: Map<String, Any?>, action: (row: Map<String, Any?>) -> Unit): DataResult<Unit> {
        return executeStream(params, builder.rowMappers.ColumnNameMapper(), action)
    }

    override fun <T : Any> forEachRowOf(
        kClass: KClass<T>,
        params: Map<String, Any?>,
        action: (obj: T) -> Unit
    ): DataResult<Unit> {
        return executeStream(params, builder.rowMappers.DataObjectMapper(kClass), action)
    }
}
