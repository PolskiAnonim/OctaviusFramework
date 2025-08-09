package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.DataFetcher
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

/**
 * Centralny komponent inicjalizujący i dostarczający usługi bazodanowe.
 *
 * Odpowiedzialny za konfigurację i tworzenie instancji usług, ale udostępnia je
 * przez interfejsy, umożliwiając luźne powiązania w aplikacji (Dependency Injection).
 */
class DatabaseSystem {
    /** Pula połączeń HikariCP z konfiguracją dla PostgreSQL */
    private val dataSource: HikariDataSource
    /** Template Spring JDBC z obsługą named parameters */
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
    private val datasourceTransactionManager: DataSourceTransactionManager
    private val typeRegistry: TypeRegistry
    private val typesConverter: PostgresToKotlinConverter
    private val rowMappers: RowMappers

    private val kotlinToPostgresConverter = KotlinToPostgresConverter()

    // Publiczne API udostępnia INTERFEJSY
    val fetcher: DataFetcher
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
        rowMappers = RowMappers(typesConverter)

        val concreteTransactionManager = DatabaseBatchExecutor(datasourceTransactionManager, namedParameterJdbcTemplate, kotlinToPostgresConverter)
        val concreteFetcher = DatabaseFetcher(namedParameterJdbcTemplate, rowMappers, kotlinToPostgresConverter)

        batchExecutor = concreteTransactionManager
        fetcher = concreteFetcher
    }
}