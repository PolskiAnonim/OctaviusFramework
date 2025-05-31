package org.octavius.novels.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager

object DatabaseManager {
    private val dataSource: HikariDataSource
    private const val jdbcUrl = "jdbc:postgresql://localhost:5430/novels_games"
    private const val username = "postgres"
    private const val password = "1234"

    // Instancja JdbcTemplate
    private var jdbcTemplate: JdbcTemplate

    // Instancja TransactionManager
    private val transactionManager: DataSourceTransactionManager

    // Instancja konwertera typów użytkownika
    private var typesConverter: UserTypesConverter

    // Instancja fabryki mapperów
    private var rowMapperFactory: RowMapperFactory

    // Instancja managera operacji formularzy
    private var databaseUpdater: DatabaseUpdater

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
        transactionManager = DataSourceTransactionManager(dataSource)

        // Inicjalizacja konwertera typów
        typesConverter = UserTypesConverter(jdbcTemplate)
        typesConverter.initialize()

        // Inicjalizacja fabryki mapperów
        rowMapperFactory = RowMapperFactory(typesConverter)

        // Inicjalizacja managera operacji formularzy
        databaseUpdater = DatabaseUpdater(dataSource, jdbcTemplate)
    }

    // Wykonanie zapytania bez paginacji - zwraca wszystkie wyniki
    fun executeQuery(
        sql: String,
        params: List<Any?> = emptyList()
    ): List<Map<ColumnInfo, Any?>> {
        return jdbcTemplate.query(sql, rowMapperFactory.createColumnInfoMapper(), *params.toTypedArray())
    }

    // Wykonanie zapytania z paginacją
    fun executePagedQuery(
        sql: String,
        params: List<Any?> = emptyList(),
        page: Int = 1,
        pageSize: Int = 10
    ): Pair<List<Map<ColumnInfo, Any?>>, Long> {
        // Pobranie całkowitej liczby wyników
        val totalCount = getTotalCount(sql, params)

        // Pobranie wyników dla bieżącej strony
        val offset = (page - 1) * pageSize
        val pagedQuery = "$sql LIMIT $pageSize OFFSET $offset"

        val results = executeQuery(pagedQuery, params)
        val totalPages = if (totalCount > 0) (totalCount - 1) / pageSize + 1 else 1

        return Pair(results, totalPages)
    }

    // Pomocnicza metoda do pobrania całkowitej liczby wyników
    private fun getTotalCount(sql: String, params: List<Any?>): Long {
        val countQuery = "SELECT COUNT(*) AS counted_query FROM ($sql) AS count_subquery"
        return jdbcTemplate.queryForObject(
            countQuery,
            Long::class.java,
            *params.toTypedArray()
        ) ?: 0L
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