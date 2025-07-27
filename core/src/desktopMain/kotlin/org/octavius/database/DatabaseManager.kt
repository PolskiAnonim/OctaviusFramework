package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.config.Config
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

/**
 * Centralny menedżer połączeń i operacji bazodanowych.
 *
 * Singleton object odpowiedzialny za:
 * - Konfigurację i zarządzanie poolą połączeń HikariCP
 * - Inicjalizację całego ekosystemu bazy danych (TypeRegistry, mappers, fetcher, updater)
 * - Zapewnienie dostępu do komponentów bazodanowych dla całej aplikacji
 * - Obsługę transakcji i operacji na wielu tabelach
 * - Automatyczne ustawianie search_path dla wielu schematów PostgreSQL
 */

/**
 * Główny menedżer systemu bazodanowego aplikacji.
 *
 * Konfiguruje połączenie z PostgreSQL z obsługą:
 * - Pool połączeń HikariCP z konfiguracją dla 10 połączeń
 * - Automatyczne ustawienie search_path na public, asian_media, games
 * - Pełny system typów PostgreSQL (TypeRegistry + konwertery)
 * - Zaawansowane mapowanie wyników (RowMappers)
 * - Transakcyjne operacje aktualizacji (DatabaseUpdater)
 * - Wydajne pobieranie danych (DatabaseFetcher)
 *
 * Konfiguracja połączenia z EnvConfig:
 * - URL, username, password z pliku .env
 * - Obsługa wielu schematów PostgreSQL
 * - Automatyczna inicjalizacja wszystkich komponentów
 *
 * Przykład użycia:
 * ```kotlin
 * val users = DatabaseManager.getFetcher()
 *     .fetchPagedList("users", "*", 0, 10, "active = true")
 *
 * DatabaseManager.updateDatabase(listOf(
 *     SaveOperation.Insert("users", userData)
 * ))
 * ```
 */
object DatabaseManager {
    /** Pula połączeń HikariCP z konfiguracją dla PostgreSQL */
    private val dataSource: HikariDataSource

    /** Template Spring JDBC z obsługą named parameters */
    private var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    /** Menedżer transakcji Spring do obsługi operacji ACID */
    private val transactionManager: DataSourceTransactionManager

    /** Rejestr typów PostgreSQL skanowany ze schematu bazy */
    private var typeRegistry: TypeRegistry

    /** Konwerter typów PostgreSQL na typy Kotlin */
    private var typesConverter: DatabaseToKotlinTypesConverter

    /** Fabryka mapperów do konwersji ResultSet na obiekty Kotlin */
    private var rowMappers: RowMappers

    /** Komponent do wykonywania operacji UPDATE/INSERT/DELETE */
    private var databaseTransactionManager: DatabaseTransactionManager

    /** Komponent do wykonywania operacji SELECT z zaawansowanymi funkcjami */
    private var databaseFetcher: DatabaseFetcher

    /**
     * Inicjalizacja całego systemu bazodanowego.
     *
     * Kolejność inicjalizacji jest ważna ze względu na zależności:
     * 1. HikariCP DataSource z konfiguracją połączenia
     * 2. Spring JDBC Template i Transaction Manager
     * 3. TypeRegistry (skanuje typy z bazy)
     * 4. TypesConverter (używa TypeRegistry)
     * 5. RowMappers (używa TypesConverter)
     * 6. DatabaseUpdater i DatabaseFetcher (używa RowMappers)
     */
    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = Config.dbUrl
            this.username = Config.dbUsername
            this.password = Config.dbPassword
            maximumPoolSize = 10
            this.connectionInitSql = "SET search_path TO public, asian_media, games"
        }
        dataSource = HikariDataSource(config)

        // Inicjalizacja JdbcTemplate
        namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        transactionManager = DataSourceTransactionManager(dataSource)

        // Inicjalizacja rejestru typów
        typeRegistry = TypeRegistry(namedParameterJdbcTemplate)

        // Inicjalizacja konwertera typów
        typesConverter = DatabaseToKotlinTypesConverter(typeRegistry)

        // Inicjalizacja fabryki mapperów
        rowMappers = RowMappers(typesConverter)

        // Inicjalizacja managera operacji formularzy
        databaseTransactionManager = DatabaseTransactionManager(transactionManager, namedParameterJdbcTemplate, ParameterExpandHelper())
        databaseFetcher = DatabaseFetcher(namedParameterJdbcTemplate, rowMappers)
    }

    /**
     * Zwraca instancję DatabaseFetcher do operacji SELECT.
     * Użyj tego komponentu do wszystkich operacji odczytu danych.
     *
     * @return Skonfigurowany DatabaseFetcher.
     */
    fun getFetcher(): DatabaseFetcher {
        return databaseFetcher
    }

    /**
     * Zwraca instancję DatabaseTransactionManager do operacji modyfikujących.
     * Użyj tego komponentu do wykonywania atomowych transakcji.
     *
     * @return Skonfigurowany DatabaseTransactionManager.
     */
    fun getTransactionManager(): DatabaseTransactionManager {
        return databaseTransactionManager
    }
}