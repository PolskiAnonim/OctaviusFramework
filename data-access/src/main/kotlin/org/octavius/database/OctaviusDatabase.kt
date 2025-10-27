package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.octavius.data.DataAccess
import org.octavius.database.type.*
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.support.JdbcTransactionManager
import javax.sql.DataSource
import kotlin.time.measureTime

/**
 * System zarządzania bazą danych - centralny punkt dostępu do usług bazodanowych.
 *
 * Inicjalizuje wszystkie niezbędne komponenty do pracy z bazą danych.
 *
 * Odpowiada za:
 * - Konfigurację puli połączeń HikariCP z PostgreSQL (dla metody fromConfig)
 * - Inicjalizację menedżera transakcji Spring
 * - Automatyczne ładowanie rejestru typów z bazy danych i classpath
 * - Udostępnienie usług dostępu do danych przez interfejs [DataAccess]
 *
 */
object OctaviusDatabase {
    private val logger = KotlinLogging.logger {}

    fun fromConfig(config: DatabaseConfig): DataAccess {
        logger.info { "Initializing DataSource..." }
        // 1. Zależne od konfiguracji ustawienie `connectionInitSql`
        val connectionInitSql = if (config.setSearchPath && config.dbSchemas.isNotEmpty()) {
            val schemas = config.dbSchemas.joinToString(", ")
            logger.debug { "Setting connectionInitSql to 'SET search_path TO $schemas'" }
            "SET search_path TO $schemas"
        } else {
            logger.debug { "connectionInitSql will not be set." }
            null
        }

        logger.debug { "Configuring HikariCP datasource with URL: ${config.dbUrl}" }
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.dbUrl
            username = config.dbUsername
            password = config.dbPassword
            maximumPoolSize = 10
            this.connectionInitSql = connectionInitSql
        }
        val dataSource = HikariDataSource(hikariConfig)
        logger.debug { "HikariCP datasource initialized with pool size: ${hikariConfig.maximumPoolSize}" }


        return fromDataSource(
            dataSource = dataSource,
            packagesToScan = config.packagesToScan,
            dbSchemas = config.dbSchemas
        )
    }

    fun fromDataSource(
        dataSource: DataSource,
        packagesToScan: List<String>,
        dbSchemas: List<String>
    ): DataAccess {
        logger.info { "Initializing OctaviusDatabase..." }

        val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        val transactionManager = JdbcTransactionManager(dataSource)

        logger.debug { "Loading type registry from database..." }
        val typeRegistry: TypeRegistry
        val typeRegistryLoadTime = measureTime {
            val loader = TypeRegistryLoader(
                jdbcTemplate,
                packagesToScan,
                dbSchemas
            )
            typeRegistry = runBlocking {
                loader.load()
            }
        }
        logger.debug { "Type registry loaded successfully in ${typeRegistryLoadTime.inWholeMilliseconds}ms" }

        logger.debug { "Initializing converters and mappers" }
        val kotlinToPostgresConverter = KotlinToPostgresConverter(typeRegistry)
        val resultSetValueExtractor = ResultSetValueExtractor(typeRegistry)
        val rowMappers = RowMappers(resultSetValueExtractor)

        logger.info { "OctaviusDatabase initialization completed" }
        return DatabaseAccess(
            jdbcTemplate,
            transactionManager,
            rowMappers,
            kotlinToPostgresConverter
        )
    }
}