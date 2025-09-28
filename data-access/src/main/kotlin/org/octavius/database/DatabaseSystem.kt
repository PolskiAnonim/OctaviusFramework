package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.octavius.data.DataAccess
import org.octavius.database.DatabaseAccess
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.database.type.PostgresToKotlinConverter
import org.octavius.database.type.TypeRegistry
import org.octavius.database.type.TypeRegistryLoader
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import kotlin.time.measureTime

/**
 * System zarządzania bazą danych - centralny punkt dostępu do usług bazodanowych.
 *
 * Odpowiada za:
 * - Konfigurację puli połączeń HikariCP z PostgreSQL
 * - Inicjalizację menedżera transakcji Spring
 * - Automatyczne ładowanie rejestru typów z bazy danych i classpath
 * - Udostępnienie usług dostępu do danych przez interfejs [DataAccess]
 * 
 * Singleton inicjalizowany przy pierwszym dostępie, konfiguruje wszystkie komponenty
 * wymagane do pracy z wieloschemtową bazą PostgreSQL (public, asian_media, games).
 */
class DatabaseSystem {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    
    /** Pula połączeń HikariCP z konfiguracją dla PostgreSQL */
    private val dataSource: HikariDataSource
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
    private val datasourceTransactionManager: DataSourceTransactionManager
    private val typeRegistry: TypeRegistry
    private val typesConverter: PostgresToKotlinConverter
    private val rowMappers: RowMappers
    private val kotlinToPostgresConverter: KotlinToPostgresConverter

    /** Usługa do wykonywania zapytań */
    val dataAccess: DataAccess


    init {
        logger.info { "Initializing DatabaseSystem" }
        
        logger.debug { "Configuring HikariCP datasource with URL: ${DatabaseConfig.dbUrl}" }
        val config = HikariConfig().apply {
            jdbcUrl = DatabaseConfig.dbUrl
            username = DatabaseConfig.dbUsername
            password = DatabaseConfig.dbPassword
            maximumPoolSize = 10
            connectionInitSql = "SET search_path TO public, asian_media, games"
        }
        dataSource = HikariDataSource(config)
        logger.debug { "HikariCP datasource initialized with pool size: ${config.maximumPoolSize}" }

        namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        datasourceTransactionManager = DataSourceTransactionManager(dataSource)

        logger.debug { "Loading type registry from database" }
        val typeRegistryLoadTime = measureTime {
            val loader = TypeRegistryLoader(namedParameterJdbcTemplate)
            typeRegistry = runBlocking {
                loader.load()
            }
        }
        logger.debug { "Type registry loaded successfully in ${typeRegistryLoadTime.inWholeMilliseconds}ms" } // Dodany czas

        logger.debug { "Initializing converters and mappers" }
        typesConverter = PostgresToKotlinConverter(typeRegistry)
        kotlinToPostgresConverter = KotlinToPostgresConverter(typeRegistry)
        rowMappers = RowMappers(typesConverter)

        logger.debug { "Initializing database services" }

        val concreteDataAccess = DatabaseAccess(
            namedParameterJdbcTemplate,
            datasourceTransactionManager,
            rowMappers,
            kotlinToPostgresConverter
        )

        dataAccess = concreteDataAccess
        
        logger.info { "DatabaseSystem initialization completed" }
    }
}