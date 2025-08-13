package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.DataFetcher
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

/**
 * Inicjalizuje i udostępnia podstawowe usługi bazodanowe.
 *
 * Konfiguruje pulę połączeń, menedżery transakcji i usługi dostępu do danych,
 * udostępniając je przez publiczne interfejsy `DataFetcher` i `BatchExecutor`.
 */
class DatabaseSystem {
    /** Pula połączeń HikariCP z konfiguracją dla PostgreSQL */
    private val dataSource: HikariDataSource
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
    private val datasourceTransactionManager: DataSourceTransactionManager
    private val typeRegistry: TypeRegistry
    private val typesConverter: PostgresToKotlinConverter
    private val rowMappers: RowMappers
    private val kotlinToPostgresConverter: KotlinToPostgresConverter

    /** Usługa do wykonywania zapytań odczytujących (SELECT). */
    val fetcher: DataFetcher
    /** Usługa do wykonywania transakcyjnych operacji zapisu (CUD). */
    val batchExecutor: BatchExecutor

    init {
        val config = HikariConfig().apply {
            jdbcUrl = DatabaseConfig.dbUrl
            username = DatabaseConfig.dbUsername
            password = DatabaseConfig.dbPassword
            maximumPoolSize = 10
            connectionInitSql = "SET search_path TO public, asian_media, games"
        }
        dataSource = HikariDataSource(config)

        namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        datasourceTransactionManager = DataSourceTransactionManager(dataSource)
        typeRegistry = TypeRegistry(namedParameterJdbcTemplate)
        typesConverter = PostgresToKotlinConverter(typeRegistry)
        kotlinToPostgresConverter = KotlinToPostgresConverter(typeRegistry)
        rowMappers = RowMappers(typesConverter)

        val concreteExecutor = DatabaseBatchExecutor(datasourceTransactionManager, namedParameterJdbcTemplate, kotlinToPostgresConverter)
        val concreteFetcher = DatabaseFetcher(namedParameterJdbcTemplate, rowMappers, kotlinToPostgresConverter)

        batchExecutor = concreteExecutor
        fetcher = concreteFetcher
    }
}