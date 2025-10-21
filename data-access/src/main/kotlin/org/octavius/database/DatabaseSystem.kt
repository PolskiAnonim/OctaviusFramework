package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.octavius.data.DataAccess
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.database.type.PostgresToKotlinConverter
import org.octavius.database.type.ResultSetValueExtractor
import org.octavius.database.type.TypeRegistry
import org.octavius.database.type.TypeRegistryLoader
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import kotlin.time.measureTime

/**
 * System zarządzania bazą danych - centralny punkt dostępu do usług bazodanowych.
 *
 * Przyjmuje obiekt konfiguracyjny i na jego podstawie inicjalizuje wszystkie
 * niezbędne komponenty do pracy z bazą danych.
 *
 * Odpowiada za:
 * - Konfigurację puli połączeń HikariCP z PostgreSQL
 * - Inicjalizację menedżera transakcji Spring
 * - Automatyczne ładowanie rejestru typów z bazy danych i classpath
 * - Udostępnienie usług dostępu do danych przez interfejs [DataAccess]
 *
 * @param config Konfiguracja bazy danych wczytana z pliku properties.
 */
class DatabaseSystem(private val config: DatabaseConfig) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }


    /** Pula połączeń HikariCP z konfiguracją dla PostgreSQL */
    private val dataSource: HikariDataSource
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
    private val datasourceTransactionManager: DataSourceTransactionManager
    private val typeRegistry: TypeRegistry

    /** Usługa do wykonywania zapytań */
    val dataAccess: DataAccess

    init {
        logger.info { "Initializing DatabaseSystem..." }

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
        dataSource = HikariDataSource(hikariConfig)
        logger.debug { "HikariCP datasource initialized with pool size: ${hikariConfig.maximumPoolSize}" }

        namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        datasourceTransactionManager = DataSourceTransactionManager(dataSource)

        logger.debug { "Loading type registry from database..." }
        val typeRegistryLoadTime = measureTime {
            // 2. Przekazanie konfiguracji do TypeRegistryLoader
            val loader = TypeRegistryLoader(
                namedParameterJdbcTemplate,
                packagesToScan = config.packagesToScan,
                dbSchemas = config.dbSchemas
            )
            typeRegistry = runBlocking {
                loader.load()
            }
        }
        logger.debug { "Type registry loaded successfully in ${typeRegistryLoadTime.inWholeMilliseconds}ms" }

        logger.debug { "Initializing converters and mappers" }
        val typesConverter = PostgresToKotlinConverter(typeRegistry)
        val kotlinToPostgresConverter = KotlinToPostgresConverter(typeRegistry)
        val resultSetValueExtractor = ResultSetValueExtractor(typeRegistry,typesConverter)
        val rowMappers = RowMappers(resultSetValueExtractor)

        logger.debug { "Initializing database services" }
        dataAccess = DatabaseAccess(
            namedParameterJdbcTemplate,
            datasourceTransactionManager,
            rowMappers,
            kotlinToPostgresConverter
        )

        logger.info { "DatabaseSystem initialization completed" }
    }
}