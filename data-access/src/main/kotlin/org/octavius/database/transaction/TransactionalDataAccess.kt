// W pakiecie org.octavius.database
package org.octavius.database.transaction

import org.octavius.data.TransactionalDataAccess
import org.octavius.data.builder.*
import org.octavius.database.RowMappers
import org.octavius.database.builder.*
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Wewnętrzna implementacja TransactionalDataAccess.
 * Działa w kontekście transakcji zarządzanej przez TransactionTemplate.
 */
internal class TransactionalDatabaseAccess(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
) : TransactionalDataAccess {

    override fun select(columns: String): SelectQueryBuilder =
        DatabaseSelectQueryBuilder(jdbcTemplate, rowMappers, kotlinToPostgresConverter, columns)

    override fun update(table: String): UpdateQueryBuilder =
        DatabaseUpdateQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table)

    override fun insertInto(table: String, columns: List<String>): InsertQueryBuilder =
        DatabaseInsertQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table, columns)

    override fun deleteFrom(table: String): DeleteQueryBuilder =
        DatabaseDeleteQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table)

    override fun rawQuery(sql: String): RawQueryBuilder =
        DatabaseRawQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, sql)
}