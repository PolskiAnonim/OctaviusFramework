package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.octavius.data.DataAccess
import org.octavius.database.config.DatabaseConfig
import org.octavius.database.config.DynamicDtoSerializationStrategy
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.database.type.ResultSetValueExtractor
import org.octavius.database.type.TypeRegistry
import org.octavius.database.type.TypeRegistryLoader
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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
            dbSchemas = config.dbSchemas,
            dynamicDtoStrategy = config.dynamicDtoStrategy,
            flywayBaselineVersion = config.flywayBaselineVersion,
        )
    }

    fun fromDataSource(
        dataSource: DataSource,
        packagesToScan: List<String>,
        dbSchemas: List<String>,
        dynamicDtoStrategy: DynamicDtoSerializationStrategy,
        flywayBaselineVersion: String?
    ): DataAccess {
        logger.info { "Initializing OctaviusDatabase..." }

        val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        val transactionManager = JdbcTransactionManager(dataSource)

        runMigrations(dataSource, dbSchemas, flywayBaselineVersion)
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
        val kotlinToPostgresConverter = KotlinToPostgresConverter(typeRegistry, dynamicDtoStrategy)
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

    private fun runMigrations(dataSource: DataSource, schemas: List<String>, flywayBaselineVersion: String?) {
        logger.info { "Checking database migrations..." }

        // Konfiguracja Flyway
        val flywayConfig = Flyway.configure()
            .dataSource(dataSource)
            .schemas(*schemas.toTypedArray())
            // Domyślna lokalizacja to classpath:db/migration
            .locations("classpath:db/migration")
            .createSchemas(true)

        if (flywayBaselineVersion != null) {
            flywayConfig
                .baselineOnMigrate(true)
                .baselineVersion(flywayBaselineVersion)
        }

        val flyway = flywayConfig.load()

        try {
            val result = flyway.migrate()
            if (result.migrationsExecuted > 0) {
                logger.info { "Successfully applied ${result.migrationsExecuted} migrations." }
            } else {
                logger.debug { "Database is up to date." }
            }
        } catch (e: Exception) {
            logger.error(e) { "Migration failed!" }
            throw e
        }
    }

}
