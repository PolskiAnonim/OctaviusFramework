package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.TransactionManager
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
    private val typesConverter: DatabaseToKotlinTypesConverter
    private val rowMappers: RowMappers

    private val parameterExpandHelper = ParameterExpandHelper()

    // Publiczne API udostępnia INTERFEJSY
    val fetcher: DataFetcher
    val transactionManager: TransactionManager

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
        typesConverter = DatabaseToKotlinTypesConverter(typeRegistry)
        rowMappers = RowMappers(typesConverter)

        val concreteTransactionManager = DatabaseTransactionManager(datasourceTransactionManager, namedParameterJdbcTemplate, parameterExpandHelper)
        val concreteFetcher = DatabaseFetcher(namedParameterJdbcTemplate, rowMappers, parameterExpandHelper)

        transactionManager = concreteTransactionManager
        fetcher = concreteFetcher
    }
}