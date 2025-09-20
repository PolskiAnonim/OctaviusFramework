package org.octavius.database.builder

import org.octavius.data.contract.builder.RawQueryBuilder
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
) : AbstractModificationQueryBuilder<DatabaseRawQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table = "" ), RawQueryBuilder {
    // Jest to klasa obchodząca podstawowe buildery dlatego table jest pustym stringiem

    override fun buildSql(): String = sql
}