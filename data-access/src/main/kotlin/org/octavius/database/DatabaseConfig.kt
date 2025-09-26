package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * Konfiguracja połączenia z bazą danych PostgreSQL.
 *
 * Przechowuje parametry połączenia i umożliwia ich ładowanie z pliku properties.
 * Domyślnie skonfigurowana dla lokalnego środowiska deweloperskiego.
 * 
 * Obsługiwane schematy bazy danych: public, asian_media, games.
 */
internal object DatabaseConfig {
    private val logger = KotlinLogging.logger {}
    
    // Domyślne wartości
    var dbUrl = "jdbc:postgresql://localhost:5432/octavius"
    var dbUsername = "postgres"
    var dbPassword = "1234"
    var dbSchemas: List<String>  = listOf("public", "asian_media", "games")

    /**
     * Ładuje konfigurację z pliku `properties`.
     * @param fileName Nazwa pliku w zasobach (np. "database.properties").
     */
    fun loadFromFile(fileName: String) {
        logger.info { "Loading database configuration from file: $fileName" }
        try {
            val props = Properties()
            val resourceStream = this::class.java.classLoader.getResourceAsStream(fileName)
            if (resourceStream != null) {
                props.load(resourceStream)
                
                val oldUrl = dbUrl
                dbUrl = props.getProperty("db.url")
                dbUsername = props.getProperty("db.username")
                dbPassword = props.getProperty("db.password")
                dbSchemas = props.getProperty("db.schemas").split(",")
                
                logger.info { "Database configuration loaded successfully" }
                logger.debug { "Database URL changed from '$oldUrl' to '$dbUrl'" }
                logger.debug { "Database schemas: ${dbSchemas.joinToString(", ")}" }
            } else {
                logger.warn { "Could not find properties file '$fileName'. Using default configuration" }
                logger.debug { "Default database URL: $dbUrl" }
                logger.debug { "Default schemas: ${dbSchemas.joinToString(", ")}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load database configuration from '$fileName'" }
        }
    }
}