package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.contract.DataAccess
import org.octavius.data.contract.DataResult
import org.octavius.data.contract.builder.*
import org.octavius.data.contract.transaction.TransactionPlanResults
import org.octavius.data.contract.transaction.TransactionStep
import org.octavius.database.builder.*
import org.octavius.database.transaction.TransactionPlanExecutor
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

internal class DatabaseAccess(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val transactionManager: DataSourceTransactionManager,
    private val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
) : DataAccess {

    // --- PARADYGMAT 1: Fluent Builders ---

    override fun select(columns: String): SelectQueryBuilder =
        DatabaseSelectQueryBuilder(jdbcTemplate, rowMappers, kotlinToPostgresConverter, columns)

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
//
//    // --- PARADYGMAT 3: Imperatywny Blok Transakcyjny ---
//
//    override fun <T> transaction(block: (tx: TransactionalDataAccess) -> DataResult<T>): DataResult<T> {
//        val transactionTemplate = TransactionTemplate(transactionManager)
//
//        return try {
//            transactionTemplate.execute { status ->
//                try {
//                    // Tworzymy instancję TransactionalDataAccess, która używa tego samego jdbcTemplate
//                    val transactionalScope =
//                        TransactionalDatabaseAccess(jdbcTemplate, rowMappers, kotlinToPostgresConverter)
//                    val result = block(transactionalScope)
//
//                    if (result is DataResult.Failure) {
//                        status.setRollbackOnly()
//                        logger.warn { "Transaction block returned Failure. Rolling back." }
//                    }
//                    result
//                } catch (e: Exception) {
//                    status.setRollbackOnly()
//                    logger.error(e) { "Exception caught within transaction block. Rolling back." }
//                    // Rzucamy dalej, aby transactionTemplate to obsłużył i opakował
//                    throw e
//                }
//            }!! // `execute` może zwrócić null, ale w tym przypadku nie powinien
//        } catch (e: Exception) {
//            // Złap wszystkie wyjątki, które mogły zostać rzucone z bloku transakcyjnego
//            // i opakuj je w DataResult.Failure
//            DataResult.Failure(
//                QueryExecutionException(
//                    message = "Transaction failed and was rolled back.",
//                    cause = e
//                )
//            )
//        }
//    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}