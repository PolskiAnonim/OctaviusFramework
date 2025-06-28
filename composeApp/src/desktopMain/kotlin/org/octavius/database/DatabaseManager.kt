package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.config.EnvConfig
import org.octavius.form.ColumnInfo
import org.octavius.form.SaveOperation
import org.octavius.form.TableRelation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

object DatabaseManager {
    private val dataSource: HikariDataSource

    // Instancja JdbcTemplate
    private var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    // Instancja TransactionManager
    private val transactionManager: DataSourceTransactionManager

    // Instancja rejestru typów
    private var typeRegistry: TypeRegistry
    
    // Instancja konwertera typów użytkownika
    private var typesConverter: DatabaseToKotlinTypesConverter

    // Instancja fabryki mapperów
    private var rowMappers: RowMappers

    // Podległe instancje
    private var databaseUpdater: DatabaseUpdater
    private var databaseFetcher: DatabaseFetcher

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

    fun getFetcher() : DatabaseFetcher {
        return databaseFetcher
    }

    fun getUpdater() : DatabaseUpdater {
        return databaseUpdater
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
        val tables = StringBuilder(mainTable)

        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            tables.append(" LEFT JOIN ${relation.tableName} ON ${relation.joinCondition}")
        }
        return databaseFetcher.fetchEntity(tables.toString(), "$mainTable.id = :id", mapOf("id" to id))
    }

    // Delegowanie operacji formularzy do databaseUpdater
    fun updateDatabase(databaseOperations: List<SaveOperation>) {
        databaseUpdater.updateDatabase(databaseOperations)
    }
}