package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.form.ColumnInfo
import org.octavius.form.SaveOperation
import org.octavius.form.TableRelation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

object DatabaseManager {
    private val dataSource: HikariDataSource
    private const val jdbcUrl = "jdbc:postgresql://localhost:5430/novels_games"
    private const val username = "postgres"
    private const val password = "1234"

    // Instancja JdbcTemplate
    private var jdbcTemplate: JdbcTemplate
    private var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    // Instancja TransactionManager
    private val transactionManager: DataSourceTransactionManager

    // Instancja konwertera typów użytkownika
    private var typesConverter: UserTypesConverter

    // Instancja fabryki mapperów
    private var rowMapperFactory: RowMapperFactory
    private var rowMappers: RowMappers

    // Podległe instancje
    private var databaseUpdater: DatabaseUpdater
    private var databaseFetcher: DatabaseFetcher

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@DatabaseManager.jdbcUrl
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
            this.connectionInitSql = "SET search_path TO public, asian_media, games"
        }
        dataSource = HikariDataSource(config)

        // Inicjalizacja JdbcTemplate
        jdbcTemplate = JdbcTemplate(dataSource)
        namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        transactionManager = DataSourceTransactionManager(dataSource)

        // Inicjalizacja konwertera typów
        typesConverter = UserTypesConverter(jdbcTemplate)
        typesConverter.initialize()

        // Inicjalizacja fabryki mapperów
        rowMapperFactory = RowMapperFactory(typesConverter)
        rowMappers = RowMappers(typesConverter)

        // Inicjalizacja managera operacji formularzy
        databaseUpdater = DatabaseUpdater(dataSource, jdbcTemplate)
        databaseFetcher = DatabaseFetcher(namedParameterJdbcTemplate, rowMappers)
    }

    fun getFetcher() : DatabaseFetcher {
        return databaseFetcher
    }


    // Pobranie encji z relacjami
    fun getEntityWithRelations(
        id: Int,
        tableRelations: List<TableRelation>
    ): Map<ColumnInfo, Any?> {
        if (tableRelations.isEmpty()) {
            throw IllegalArgumentException("Lista relacji tabel nie może być pusta")
        }

        val mainTable = tableRelations.first().tableName

        // Budowanie zapytania SQL z JOIN-ami
        val sqlBuilder = StringBuilder("SELECT * FROM $mainTable ")
        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            sqlBuilder.append("LEFT JOIN ${relation.tableName} ON ${relation.joinCondition} ")
        }
        sqlBuilder.append("WHERE $mainTable.id = ?")

        // Wykonanie zapytania i pobranie wyników
        return jdbcTemplate.query(sqlBuilder.toString(), rowMapperFactory.createSingleRowExtractor(), id) ?: emptyMap()
    }

    // Delegowanie operacji formularzy do databaseUpdater
    fun updateDatabase(databaseOperations: List<SaveOperation>) {
        databaseUpdater.updateDatabase(databaseOperations)
    }
}