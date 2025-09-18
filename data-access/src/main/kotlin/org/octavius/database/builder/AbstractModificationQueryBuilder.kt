package org.octavius.database.builder

import org.octavius.data.contract.DataResult
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Abstrakcyjna klasa bazowa dla builderów zapytań modyfikujących dane (INSERT, UPDATE, DELETE).
 * Dziedziczy po [AbstractQueryBuilder], aby wspierać klauzulę RETURNING.
 */
internal abstract class AbstractModificationQueryBuilder<T : AbstractModificationQueryBuilder<T>>(
    jdbcTemplate: NamedParameterJdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    protected val table: String,
) : AbstractQueryBuilder<T>(jdbcTemplate, kotlinToPostgresConverter, rowMappers) {

    protected var returningClause: String? = null

    // Wspólna metoda returning()
    @Suppress("UNCHECKED_CAST")
    fun returning(columns: String): T = apply {
        this.returningClause = columns
    } as T

    protected fun buildReturningClause(): String {
        return returningClause?.let { " RETURNING $it" } ?: ""
    }

    /**
     * Wykonuje zapytanie modyfikujące (bez RETURNING) i zwraca liczbę zmienionych wierszy.
     * Rzuca wyjątek, jeśli klauzula RETURNING została użyta - w takim przypadku należy
     * użyć metod `toList()`, `toSingle()` itp.
     */
    fun execute(params: Map<String, Any?>): DataResult<Int> {
        if (returningClause != null) {
            throw IllegalStateException("Use toList(), toSingle(), etc. when RETURNING clause is specified.")
        }
        val sql = buildSql()
        return execute(sql, params) { expandedSql, expandedParams ->
            val affectedRows = jdbcTemplate.update(expandedSql, expandedParams)
            DataResult.Success(affectedRows)
        }
    }
}