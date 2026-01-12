package org.octavius.database.builder

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataResult
import org.octavius.data.builder.StreamingTerminalMethods
import org.octavius.data.exception.QueryExecutionException
import org.octavius.database.type.PositionalQuery
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterUtils
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
        var positionalQuery: PositionalQuery? = null

        return try {
            positionalQuery = builder.kotlinToPostgresConverter.expandParametersInQuery(originalSql, params)

            logger.debug {
                """
                Executing query (original): $originalSql with params: $params
                  -> (expanded): ${positionalQuery.sql} with positional params: ${positionalQuery.params}
                """.trimIndent()
            }



            val pss = PreparedStatementSetter { ps ->
                // Ustawiamy parametry (pętla jest tu schowana, w dedykowanym miejscu)
                positionalQuery.params.forEachIndexed { index, value ->
                    ps.setObject(index + 1, value)
                }

                // Konfiguracja streamingu (teraz też jest w logicznym miejscu)
                if (ps.connection.autoCommit) {
                    logger.warn {
                        "POTENTIAL PERFORMANCE ISSUE: Streaming query executed with autoCommit=true. " +
                                "PostgreSQL driver will ignore fetchSize=$fetchSize and load all rows into RAM. " +
                                "Wrap this call in DataAccess.transaction { ... }."
                    }
                }
                ps.fetchSize = this.fetchSize
            }

            // ZMIANA: Krok 2 - Przygotuj ResultSetExtractor
            // Jego jedynym zadaniem jest iteracja po wynikach i wywołanie akcji
            val rse = ResultSetExtractor<Unit> { rs ->
                var rowNum = 0
                while (rs.next()) {
                    val mappedItem = rowMapper.mapRow(rs, rowNum++)
                    action(mappedItem)
                }
            }

            builder.jdbcTemplate.query(positionalQuery.sql, pss, rse)


            DataResult.Success(Unit)
        } catch (e: Exception) {
            val executionException = QueryExecutionException(
                sql = originalSql,
                params = params,
                expandedSql = positionalQuery?.sql,
                expandedParams = positionalQuery?.params,
                message = "Streaming query failed.",
                cause = e
            )
            logger.error(executionException) { "Database error executing streaming query" }
            DataResult.Failure(executionException)
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
