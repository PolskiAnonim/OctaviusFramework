package org.octavius.database.builder

import org.octavius.data.builder.RawQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Wykonuje surowe zapytanie SQL, które zwraca wyniki.
 * Umożliwia przekazanie dowolnego SQLa do wykonania oferując wygodne metody terminalne
 */
internal class DatabaseRawQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    private val sql: String
) : AbstractQueryBuilder<RawQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, null), RawQueryBuilder {
    override val canReturnResultsByDefault = true
    override fun buildSql(): String = sql

    // UWAGA! Ta funkcja nie ma realnie żadnego zastosowania i zwraca technicznie taki sam obiekt jaki był
    override fun copy(): DatabaseRawQueryBuilder {
        val newBuilder = DatabaseRawQueryBuilder(
            jdbcTemplate,
            kotlinToPostgresConverter,
            rowMappers,
            sql = this.sql
        )
        // Nie kopiujemy stanu bazowego ze względu na fakt że interfejs uniemożliwia nawet jego wykorzystanie
        return newBuilder
    }
}