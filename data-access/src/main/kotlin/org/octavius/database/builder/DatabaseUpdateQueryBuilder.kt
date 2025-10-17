package org.octavius.database.builder

import org.octavius.data.builder.UpdateQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal class DatabaseUpdateQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    table: String
) : AbstractQueryBuilder<UpdateQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table), UpdateQueryBuilder {
    override val canReturnResultsByDefault = false
    private val setClauses = mutableMapOf<String, String>()
    private var fromClause: String? = null
    private var whereClause: String? = null

    override fun setExpression(column: String, value: String): UpdateQueryBuilder = apply {
        // Podobnie jak niżej, ale dla pojedynczej kolumny
        setClauses[column] = value
    }

    override fun setExpressions(values: Map<String, String>): UpdateQueryBuilder = apply {
        values.forEach { (key, value) ->
            setClauses[key] = value
        }
    }

    override fun setValues(values: Map<String, Any?>): UpdateQueryBuilder {
        // Generujemy mapę placeholderów dla istniejącej metody setExpressions
        val placeholders = values.keys.associateWith { key -> ":$key" }
        return this.setExpressions(placeholders)
    }

    override fun setValue(column: String): UpdateQueryBuilder {
        return this.setExpression(column, ":$column")
    }

    override fun from(tables: String): UpdateQueryBuilder = apply {
        this.fromClause = tables
    }

    override fun where(condition: String): UpdateQueryBuilder = apply {
        this.whereClause = condition
    }

    override fun buildSql(): String {
        if (setClauses.isEmpty()) {
            throw IllegalStateException("Cannot build an UPDATE statement without a SET clause.")
        }
        if (whereClause.isNullOrBlank()) {
            throw IllegalStateException("Cannot build an UPDATE statement without a WHERE clause for safety.")
        }

        val sql = StringBuilder(buildWithClause())
        sql.append("UPDATE $table SET ")
        sql.append(setClauses.entries.joinToString(", ") { "${it.key} = ${it.value}" })

        fromClause?.let { sql.append(" FROM $it") }

        sql.append(" WHERE $whereClause")
        sql.append(buildReturningClause())

        return sql.toString()
    }

    override fun copy(): DatabaseUpdateQueryBuilder {
        // 1. Stwórz nową, "czystą" instancję za pomocą głównego konstruktora
        val newBuilder = DatabaseUpdateQueryBuilder(
            this.jdbcTemplate,
            this.kotlinToPostgresConverter,
            this.rowMappers,
            this.table!! // Wynika z faktu że w AbstractQueryBuilder jest to własność nullowalna
        )

        // 2. Skopiuj stan z klasy bazowej używając metody pomocniczej
        newBuilder.copyBaseStateFrom(this)

        // 3. Skopiuj stan specyficzny dla TEJ klasy
        newBuilder.fromClause = this.fromClause
        newBuilder.whereClause = this.whereClause
        newBuilder.setClauses.putAll(setClauses)

        // 4. Zwróć w pełni skonfigurowaną kopię
        return newBuilder
    }
}