package org.octavius.database.builder

import org.octavius.data.contract.builder.UpdateQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal class DatabaseUpdateQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    table: String
) : AbstractModificationQueryBuilder<DatabaseUpdateQueryBuilder> (
    jdbcTemplate,
    kotlinToPostgresConverter,
    rowMappers,
    table
), UpdateQueryBuilder {

    private val setClauses = mutableMapOf<String, String>()
    private var fromClause: String? = null
    private var whereClause: String? = null

    override fun set(values: Map<String, String>): UpdateQueryBuilder = apply {
        values.forEach { (key, value) ->
            setClauses[key] = value
        }
    }

    override fun set(column: String, value: String): UpdateQueryBuilder = apply {
        // Podobnie jak wyżej, ale dla pojedynczej kolumny
        setClauses[column] = value
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
}