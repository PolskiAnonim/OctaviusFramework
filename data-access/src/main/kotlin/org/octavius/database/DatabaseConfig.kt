package org.octavius.database

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.util.*

/**
 * Niezmienna (immutable) konfiguracja połączenia z bazą danych PostgreSQL.
 *
 * Przechowuje parametry połączenia oraz konfigurację dla komponentów systemu.
 *
 * @property dbUrl Adres URL połączenia JDBC.
 * @property dbUsername Nazwa użytkownika bazy danych.
 * @property dbPassword Hasło użytkownika bazy danych.
 * @property dbSchemas Lista schematów, które mają być obsługiwane.
 * @property setSearchPath Czy HikariCP ma ustawiać `search_path` przy inicjalizacji połączenia na wszystkie schematy.
 * @property packagesToScan Lista pakietów do przeskanowania przez ClassGraph w poszukiwaniu adnotacji typów.
 */
data class DatabaseConfig(
    val dbUrl: String,
    val dbUsername: String,
    val dbPassword: String,
    val dbSchemas: List<String>,
    val setSearchPath: Boolean,
    val packagesToScan: List<String>
) {
    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Ładuje konfigurację z pliku `properties` i tworzy instancję `DatabaseConfig`.
         *
         * @param fileName Nazwa pliku w zasobach (np. "database.properties").
         * @return Utworzona, niezmienna instancja [DatabaseConfig].
         * @throws IllegalArgumentException jeśli plik konfiguracyjny nie zostanie znaleziony
         *         lub brakuje w nim wymaganego klucza.
         * @throws IOException jeśli wystąpi błąd podczas odczytu pliku.
         */
        fun loadFromFile(fileName: String): DatabaseConfig {
            logger.info { "Loading database configuration from file: $fileName" }
            val props = Properties()

            val resourceStream = this::class.java.classLoader.getResourceAsStream(fileName)
                ?: throw IllegalArgumentException("Could not find properties file '$fileName' in resources.")

            resourceStream.use { props.load(it) }

            // --- Podstawowa konfiguracja bazy ---
            val url = props.getProperty("db.url")
                ?: throw IllegalArgumentException("Missing required property 'db.url' in '$fileName'")
            val username = props.getProperty("db.username")
                ?: throw IllegalArgumentException("Missing required property 'db.username' in '$fileName'")
            val password = props.getProperty("db.password")
                ?: throw IllegalArgumentException("Missing required property 'db.password' in '$fileName'")
            val schemasString = props.getProperty("db.schemas")
                ?: throw IllegalArgumentException("Missing required property 'db.schemas' in '$fileName'")
            val schemas = schemasString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            // --- Konfiguracja specyficzna dla aplikacji ---
            val setSearchPath = props.getProperty("db.setSearchPath", "true").toBoolean()

            val packagesString = props.getProperty("db.packagesToScan")
                ?: throw IllegalArgumentException("Missing required property 'db.packagesToScan' in '$fileName'")
            val packages = packagesString.split(",").map { it.trim() }.filter { it.isNotEmpty() }


            logger.info { "Database configuration loaded successfully" }
            logger.debug { "Database URL: '$url'" }
            logger.debug { "Database schemas: ${schemas.joinToString()}" }
            logger.debug { "Set search_path on connect: $setSearchPath" }
            logger.debug { "Packages to scan for types: ${packages.joinToString()}" }

            return DatabaseConfig(
                dbUrl = url,
                dbUsername = username,
                dbPassword = password,
                dbSchemas = schemas,
                setSearchPath = setSearchPath,
                packagesToScan = packages
            )
        }
    }
}