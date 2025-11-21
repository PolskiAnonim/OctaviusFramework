package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.QueryOperations
import org.octavius.data.builder.*
import org.octavius.data.exception.DatabaseException
import org.octavius.data.exception.QueryExecutionException
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionPlanResult
import org.octavius.data.transaction.TransactionPropagation
import org.octavius.database.builder.*
import org.octavius.database.transaction.TransactionPlanExecutor
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

internal class DatabaseAccess(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val transactionManager: DataSourceTransactionManager,
    private val rowMappers: RowMappers,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter
) : DataAccess {
    val transactionPlanExecutor = TransactionPlanExecutor(transactionManager)
    // --- Implementacja QueryOperations (dla pojedynczych zapytań i użycia w transakcji) ---

    override fun select(vararg columns: String): SelectQueryBuilder {
        return DatabaseSelectQueryBuilder(jdbcTemplate, rowMappers, kotlinToPostgresConverter, columns.joinToString(",\n"))
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

    //--- Implementacja zarządzania transakcjami ---

    override fun executeTransactionPlan(
        plan: TransactionPlan,
        propagation: TransactionPropagation
    ): DataResult<TransactionPlanResult> {
        return transactionPlanExecutor.execute(plan, propagation)
    }

    override fun <T> transaction(
        propagation: TransactionPropagation,
        block: (tx: QueryOperations) -> DataResult<T>
    ): DataResult<T> {
        // Tworzymy i konfigurujemy szablon transakcji
        val transactionTemplate = TransactionTemplate(transactionManager).apply {
            propagationBehavior = when (propagation) {
                TransactionPropagation.REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED
                TransactionPropagation.REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW
                TransactionPropagation.NESTED -> TransactionDefinition.PROPAGATION_NESTED
            }
        }
        return transactionTemplate.execute { status ->
            try {
                // `this` jest instancją `QueryOperations`, więc przekazujemy go bezpośrednio.
                val result = block(this)

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
                        params = mapOf(),
                        cause = e
                    )
                )
            }
        }!!
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}