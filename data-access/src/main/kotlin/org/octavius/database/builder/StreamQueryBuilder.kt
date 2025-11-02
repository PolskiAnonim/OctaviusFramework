package org.octavius.database.builder

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.octavius.data.DataResult
import org.octavius.data.builder.StreamingTerminalMethods
import org.octavius.data.exception.DatabaseException
import org.octavius.data.exception.QueryExecutionException
import org.springframework.jdbc.core.PreparedStatementCallback
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
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
        var expandedSql: String? = null
        var expandedParams: Map<String, Any?>? = null

        return try {
            val expanded = builder.kotlinToPostgresConverter.expandParametersInQuery(originalSql, params)
            expandedSql = expanded.expandedSql
            expandedParams = expanded.expandedParams

            logger.debug { "Executing streaming query (expanded): $expandedSql with params: $expandedParams" }

            // Krok 2: Konwersja z :nazwa na ?
            val parsedSql = NamedParameterUtils.parseSqlStatement(expandedSql)
            val paramsInOrder =
                NamedParameterUtils.buildValueArray(parsedSql, MapSqlParameterSource(expandedParams), null)
            val sqlWithQuestionMarks = NamedParameterUtils.substituteNamedParameters(parsedSql, null)

            // Krok 3: Użycie PreparedStatementCallback
            val callback = PreparedStatementCallback { ps ->
                ps.fetchSize = this.fetchSize // Ustawiamy streaming!

                for ((index, value) in paramsInOrder.withIndex()) {
                    ps.setObject(index + 1, value)
                }

                val rs: ResultSet = ps.executeQuery()
                var rowNum = 0
                while (rs.next()) {
                    val mappedItem = rowMapper.mapRow(rs, rowNum++)
                    action(mappedItem) // Wywołujemy akcję użytkownika dla każdego wiersza
                }
            }

            // Krok 4: Wykonanie
            builder.jdbcTemplate.jdbcOperations.execute(sqlWithQuestionMarks, callback)

            DataResult.Success(Unit)
        } catch (e: DatabaseException) {
            // Logujemy z pełnym kontekstem
            logger.error(e) { "Database error executing streaming query: $expandedSql with params: $expandedParams" }
            DataResult.Failure(e)
        } catch (e: Exception) {
            // Logujemy i tworzymy wyjątek z pełnym kontekstem
            logger.error(e) { "Unexpected error executing streaming query: $expandedSql with params: $expandedParams" }
            DataResult.Failure(
                QueryExecutionException(
                    "Streaming query failed.",
                    sql = expandedSql ?: originalSql,
                    params = expandedParams ?: params,
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

    override fun toFlow(params: Map<String, Any?>): Flow<Map<String, Any?>> {
        return channelFlow {
            // Używamy withContext(Dispatchers.IO), bo operacje JDBC są blokujące
            // i MUSZĄ być wykonane na wątku przeznaczonym do takich operacji.
            withContext(Dispatchers.IO) {
                val result = executeStream(params, builder.rowMappers.ColumnNameMapper()) { item ->
                    trySend(item)
                }
                // Jeśli executeStream zwróci błąd, zamknij Flow z tym błędem
                if (result is DataResult.Failure) {
                    close(result.error)
                }
            }
        }
    }

    override fun <T : Any> toFlowOf(
        kClass: KClass<T>,
        params: Map<String, Any?>
    ): Flow<T> {
        return channelFlow {
            // Używamy withContext(Dispatchers.IO), bo operacje JDBC są blokujące
            // i MUSZĄ być wykonane na wątku przeznaczonym do takich operacji.
            withContext(Dispatchers.IO) {
                val result = executeStream(params, builder.rowMappers.DataObjectMapper(kClass)) { item ->
                    trySend(item)
                }
                // Jeśli executeStream zwróci błąd, zamknij Flow z tym błędem
                if (result is DataResult.Failure) {
                    close(result.error)
                }
            }
        }
    }
}