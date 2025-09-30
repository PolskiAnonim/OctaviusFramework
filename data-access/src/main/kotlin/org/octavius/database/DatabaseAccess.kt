package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.TransactionalDataAccess
import org.octavius.data.builder.*
import org.octavius.data.transaction.TransactionPlanResults
import org.octavius.data.transaction.TransactionStep
import org.octavius.database.builder.*
import org.octavius.database.transaction.TransactionPlanExecutor
import org.octavius.database.transaction.TransactionalDatabaseAccess
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.exception.DatabaseException
import org.octavius.exception.QueryExecutionException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate

internal class DatabaseAccess(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val transactionManager: DataSourceTransactionManager,
    private val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
) : DataAccess {

    // --- PARADYGMAT 1: Fluent Builders ---

    override fun select(vararg columns: String): SelectQueryBuilder {
        // Łączymy wszystkie argumenty w jeden string, oddzielając je ", "
        val selectClause = columns.joinToString(", ")
        return DatabaseSelectQueryBuilder(jdbcTemplate, rowMappers, kotlinToPostgresConverter, selectClause)
    }

    override fun update(table: String): UpdateQueryBuilder {
        return DatabaseUpdateQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table)
    }

    override fun insertInto(table: String, columns: List<String>): InsertQueryBuilder {
        return DatabaseInsertQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table, columns)
    }

    override fun deleteFrom(table: String): DeleteQueryBuilder {
        return DatabaseDeleteQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table)
    }

    override fun rawQuery(sql: String): RawQueryBuilder {
        return DatabaseRawQueryBuilder(jdbcTemplate, kotlinToPostgresConverter, rowMappers, sql)
    }

     //--- PARADYGMAT 2: Deklaratywny Batch ---

    override fun executeTransactionPlan(steps: List<TransactionStep<*>>): DataResult<TransactionPlanResults> {
        val transactionPlanExecutor = TransactionPlanExecutor(
            transactionManager
        )
        return transactionPlanExecutor.execute(steps)
    }

    // --- PARADYGMAT 3: Imperatywny Blok Transakcyjny ---

    override fun <T> transaction(block: (tx: TransactionalDataAccess) -> DataResult<T>): DataResult<T> {
        val transactionTemplate = TransactionTemplate(transactionManager)

        // Użycie !! jest bezpieczne, bo `execute` zwraca null tylko, gdy nie ma `block`
        return transactionTemplate.execute { status ->
            try {
                // Tworzymy instancję TransactionalDataAccess, która używa tego samego jdbcTemplate
                val transactionalScope =
                    TransactionalDatabaseAccess(jdbcTemplate, rowMappers, kotlinToPostgresConverter)
                val result = block(transactionalScope)

                // Jeśli jakakolwiek operacja wewnątrz bloku zwróciła Failure, wycofujemy transakcję.
                // To pozwala na kontrolowane wycofanie bez rzucania wyjątku!
                if (result is DataResult.Failure) {
                    logger.warn { "Transaction block returned Failure. Rolling back transaction." }
                    status.setRollbackOnly()
                }
                result // Zwracamy oryginalny wynik (Success lub Failure)
            } catch (e: DatabaseException) {
                // Łapiemy nasze własne wyjątki, żeby nie opakowywać ich ponownie
                status.setRollbackOnly()
                logger.error(e) { "A DatabaseException was thrown inside the transaction block. Rolling back." }
                DataResult.Failure(e)
            } catch (e: Exception) {
                // Łapiemy każdy inny, nieoczekiwany wyjątek
                status.setRollbackOnly()
                logger.error(e) { "An unexpected exception was thrown inside the transaction block. Rolling back." }
                // Opakowujemy go w nasze standardowe Failure
                DataResult.Failure(
                    QueryExecutionException(
                        "Transaction failed due to an unexpected error: ${e.message}",
                        sql = "N/A",
                        mapOf(),
                        e
                    )
                )
            }
        }!!
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}