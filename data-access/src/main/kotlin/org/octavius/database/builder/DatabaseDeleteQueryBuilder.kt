package org.octavius.database.builder

import org.octavius.data.builder.DeleteQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal class DatabaseDeleteQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    table: String
) : AbstractQueryBuilder<DatabaseDeleteQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table), DeleteQueryBuilder {
    override val canReturnResultsByDefault = false
    private var whereClause: String? = null
    private var usingClause: String? = null

    override fun using(tables: String): DeleteQueryBuilder = apply {
        this.usingClause = tables
    }

    override fun where(condition: String): DeleteQueryBuilder = apply {
        this.whereClause = condition
    }

    override fun buildSql(): String {
        if (whereClause.isNullOrBlank()) {
            throw IllegalStateException("Cannot build a DELETE statement without a WHERE clause for safety.")
        }

        val sql = StringBuilder(buildWithClause())
        sql.append("DELETE FROM $table")
        usingClause?.let { sql.append(" USING $it") }
        sql.append(" WHERE $whereClause")
        sql.append(buildReturningClause())

        return sql.toString()
    }
}