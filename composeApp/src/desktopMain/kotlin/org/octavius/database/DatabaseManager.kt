package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.config.EnvConfig
import org.octavius.form.ColumnInfo
import org.octavius.form.SaveOperation
import org.octavius.form.TableRelation
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
    private var databaseUpdater: DatabaseUpdater

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
            this.jdbcUrl = EnvConfig.dbUrl
            this.username = EnvConfig.dbUsername
            this.password = EnvConfig.dbPassword
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
        databaseUpdater = DatabaseUpdater(transactionManager, namedParameterJdbcTemplate)
        databaseFetcher = DatabaseFetcher(namedParameterJdbcTemplate, rowMappers)
    }

    /**
     * Zwraca instancję DatabaseFetcher do operacji SELECT.
     *
     * @return Skonfigurowany DatabaseFetcher z pełnym systemem typów
     */
    fun getFetcher(): DatabaseFetcher {
        return databaseFetcher
    }

    /**
     * Zwraca instancję DatabaseUpdater do operacji modyfikujących.
     *
     * @return Skonfigurowany DatabaseUpdater z obsługą transakcji
     */
    fun getUpdater(): DatabaseUpdater {
        return databaseUpdater
    }


    /**
     * Pobiera encję z automatycznym łączeniem tabel wedlug relacji.
     *
     * Buduje złożone zapytanie SQL z LEFT JOIN na podstawie listy TableRelation
     * i pobiera wynik jako mapę ColumnInfo.
     *
     * @param id Identyfikator głównej encji
     * @param tableRelations Lista relacji tabel do połączenia
     * @return Mapa obiektów ColumnInfo do wartości z połączonych tabel
     *
     * @throws IllegalArgumentException gdy lista relacji jest pusta
     *
     * Przykład:
     * ```kotlin
     * val relations = listOf(
     *     TableRelation("users", ""),
     *     TableRelation("profiles", "users.id = profiles.user_id"),
     *     TableRelation("addresses", "profiles.id = addresses.profile_id")
     * )
     * val entity = getEntityWithRelations(123, relations)
     * ```
     */
    fun getEntityWithRelations(
        id: Int,
        tableRelations: List<TableRelation>
    ): Map<ColumnInfo, Any?> {
        if (tableRelations.isEmpty()) {
            throw IllegalArgumentException("Lista relacji tabel nie może być pusta")
        }

        val mainTable = tableRelations.first().tableName
        val tables = StringBuilder(mainTable)

        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            tables.append(" LEFT JOIN ${relation.tableName} ON ${relation.joinCondition}")
        }
        return databaseFetcher.fetchEntity(tables.toString(), "$mainTable.id = :id", mapOf("id" to id))
    }

    /**
     * Wykonuje listę operacji bazodanowych w pojedynczej transakcji.
     *
     * Deleguje do DatabaseUpdater który obsługuje:
     * - Transakcyjność (wszystkie operacje lub żadna)
     * - Zarządzanie kluczami obcymi między operacjami
     * - Rollback przy błędach
     * - Ekspansję złożonych parametrów PostgreSQL
     *
     * @param databaseOperations Lista operacji SaveOperation do wykonania
     *
     * @throws Exception gdy którakolwiek operacja się nie powiedzie (z rollback)
     *
     * Przykład:
     * ```kotlin
     * updateDatabase(listOf(
     *     SaveOperation.Insert("users", userData, returningId = true),
     *     SaveOperation.Insert("profiles", profileData, foreignKeys = listOf(
     *         ForeignKeyReference("user_id", "users", null)
     *     ))
     * ))
     * ```
     */
    fun updateDatabase(databaseOperations: List<SaveOperation>) {
        databaseUpdater.updateDatabase(databaseOperations)
    }
}