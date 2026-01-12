package org.octavius.database.builder

import org.octavius.data.builder.DeleteQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal class DatabaseDeleteQueryBuilder(
    jdbcTemplate: JdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    table: String
) : AbstractQueryBuilder<DeleteQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table), DeleteQueryBuilder {
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
        check(!whereClause.isNullOrBlank()) { "Cannot build a DELETE statement without a WHERE clause for safety." }

        val sql = StringBuilder(buildWithClause())
        sql.append("DELETE FROM $table")
        usingClause?.let { sql.append("\nUSING $it") }
        sql.append("\nWHERE $whereClause")
        sql.append(buildReturningClause())

        return sql.toString()
    }

    override fun copy(): DatabaseDeleteQueryBuilder {
        // 1. Stwórz nową, "czystą" instancję za pomocą głównego konstruktora
        val newBuilder = DatabaseDeleteQueryBuilder(
            this.jdbcTemplate,
            this.kotlinToPostgresConverter,
            this.rowMappers,
            this.table!! // Wynika z faktu że w AbstractQueryBuilder jest to własność nullowalna
        )

        // 2. Skopiuj stan z klasy bazowej używając metody pomocniczej
        newBuilder.copyBaseStateFrom(this)

        // 3. Skopiuj stan specyficzny dla TEJ klasy
        newBuilder.whereClause = this.whereClause
        newBuilder.usingClause = this.usingClause

        // 4. Zwróć w pełni skonfigurowaną kopię
        return newBuilder
    }
}
