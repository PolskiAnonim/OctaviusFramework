package org.octavius.novels.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation
import org.octavius.novels.util.Converters.camelToSnakeCase
import org.octavius.novels.util.Converters.snakeToCamelCase
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@DatabaseManager.jdbcUrl
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)

        // Inicjalizacja JdbcTemplate
        jdbcTemplate = JdbcTemplate(dataSource)
        transactionManager = DataSourceTransactionManager(dataSource)
        // Inicjalizacja konwertera typów
        typesConverter = UserTypesConverter(jdbcTemplate)
        typesConverter.initialize()
    }

    // RowMapper dla mapowania wyników na mapy
    private val mapRowMapper = RowMapper<Map<String, Any?>> { rs, _ ->
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData

        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnTypeName(i)

            val rawValue = rs.getObject(i)
            if (rs.wasNull()) {
                data[snakeToCamelCase(columnName)] = null
            } else {
                val convertedValue = typesConverter.convertToDomainType(rawValue, columnType)
                data[snakeToCamelCase(columnName)] = convertedValue
            }
        }

        data
    }

    // Wykonanie zapytania z paginacją
    fun executeQuery(
        sql: String,
        params: List<Any?> = emptyList(),
        page: Int = 1,
        pageSize: Int = 10
    ): Pair<List<Map<String, Any?>>, Long> {
        // Pobranie całkowitej liczby wyników
        val countQuery = "SELECT COUNT(*) AS counted_query FROM ($sql)"
        val totalCount = jdbcTemplate.queryForObject(
            countQuery,
            Long::class.java,
            *params.toTypedArray()
        ) ?: 0L

        // Pobranie wyników dla bieżącej strony
        val offset = (page - 1) * pageSize
        val pagedQuery = "$sql LIMIT $pageSize OFFSET $offset"

        val results = jdbcTemplate.query(pagedQuery, mapRowMapper, *params.toTypedArray())

        return Pair(results, totalCount / pageSize + 1)
    }

    // Mapowanie ResultSet na klasę
    private fun <T : Any> mapResultSetToClass(rs: ResultSet, resultClass: KClass<T>): T {
        val constructor = resultClass.primaryConstructor!!
        val parameters = constructor.parameters

        val args = parameters.associateWith { param ->
            val columnName = param.name!!
            val columnNameSnakeCase = camelToSnakeCase(columnName)

            val metaData = rs.metaData
            val columnType = (1..metaData.columnCount)
                .firstOrNull { metaData.getColumnName(it).equals(columnNameSnakeCase, ignoreCase = true) }
                ?.let { metaData.getColumnTypeName(it) }
                ?: "unknown"

            val rawValue = rs.getObject(columnNameSnakeCase)
            if (rs.wasNull()) {
                null
            } else {
                typesConverter.convertToDomainType(rawValue, columnType)
            }
        }

        return constructor.callBy(args)
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
        return jdbcTemplate.query(sqlBuilder.toString(), ResultSetExtractor { rs ->
            val result = mutableMapOf<ColumnInfo, Any?>()

            if (rs.next()) {
                val metaData = rs.metaData
                for (i in 1..metaData.columnCount) {
                    val columnName = metaData.getColumnName(i)
                    val tableName = metaData.getTableName(i)
                    val columnType = metaData.getColumnTypeName(i)

                    val rawValue = rs.getObject(i)
                    if (rs.wasNull()) {
                        result[ColumnInfo(tableName, columnName)] = null
                    } else {
                        val convertedValue = typesConverter.convertToDomainType(rawValue, columnType)
                        result[ColumnInfo(tableName, columnName)] = convertedValue
                    }
                }
            }
            result
        }, id) ?: emptyMap()
    }

    // Metody do operacji DML (Insert, Update, Delete)

    // Główna metoda do zapisywania encji
    fun updateDatabase(databaseOperations: List<SaveOperation>) {
        // Używamy TransactionTemplate zamiast ręcznego zarządzania transakcjami
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute { status ->
            try {
                for (operation in databaseOperations) {
                    when (operation) {
                        is SaveOperation.Insert -> {
                            val id = insertIntoTable(operation)
                            databaseOperations.filterIsInstance<SaveOperation.Insert>().forEach { op ->
                                op.foreignKeys.filter { it.referencedTable == operation.tableName }
                                    .forEach { it.value = id }
                            }
                        }

                        is SaveOperation.Update -> {
                            updateTable(operation)
                        }

                        is SaveOperation.Delete -> {
                            deleteFromTable(operation)
                        }
                    }
                }
            } catch (e: Exception) {
                status.setRollbackOnly()
                println("Błąd wstawiania rekordu: ${e.message}")
                throw e
            }
        }
    }

    private fun insertIntoTable(operation: SaveOperation.Insert): Int? {
        val dataColumns = operation.data.keys.toList()

        // Dodaj kolumny kluczy obcych
        val foreignKeyColumns = operation.foreignKeys
            .filter { it.value != null }
            .map { it.columnName }

        val allColumns = dataColumns + foreignKeyColumns
        val columnsString = allColumns.joinToString()
        val placeholders = allColumns.joinToString { "?" }

        val insertQuery = "INSERT INTO ${operation.tableName} ($columnsString) VALUES ($placeholders) RETURNING id"

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(insertQuery, arrayOf("id"))

            // Ustaw parametry dla zwykłych danych
            operation.data.values.forEachIndexed { index, value ->
                setStatementParameter(ps, index + 1, value.value)
            }

            // Ustaw parametry dla kluczy obcych
            val fkValues = operation.foreignKeys
                .filter { it.value != null }
                .map { it.value }

            fkValues.forEachIndexed { index, value ->
                setStatementParameter(ps, operation.data.size + index + 1, value)
            }

            ps
        }, keyHolder)

        return keyHolder.keys?.get("id") as? Int
    }

    private fun updateTable(operation: SaveOperation.Update) {
        val updatePairs = operation.data.entries.joinToString { "${it.key} = ?" }
        val updateQuery = "UPDATE ${operation.tableName} SET $updatePairs WHERE id = ?"

        jdbcTemplate.update(updateQuery) { ps ->
            // Ustaw parametry dla danych
            operation.data.values.forEachIndexed { index, value ->
                setStatementParameter(ps, index + 1, value.value)
            }

            // Ustaw parametr dla id
            ps.setInt(operation.data.size + 1, operation.id)
        }
    }

    private fun deleteFromTable(operation: SaveOperation.Delete) {
        val deleteQuery = "DELETE FROM ${operation.tableName} WHERE id = ?"
        jdbcTemplate.update(deleteQuery, operation.id)
    }

    // Pomocnicza metoda do ustawiania parametrów w PreparedStatement
    private fun setStatementParameter(ps: java.sql.PreparedStatement, index: Int, value: Any?) {
        when (value) {
            null -> ps.setNull(index, Types.NULL)
            is Int -> ps.setInt(index, value)
            is Long -> ps.setLong(index, value)
            is String -> ps.setString(index, value)
            is Boolean -> ps.setBoolean(index, value)
            is Double -> ps.setDouble(index, value)
            is List<*> -> {
                // Dla list, konwertujemy na tablicę
                val array = ps.connection.createArrayOf("text", value.toTypedArray())
                ps.setArray(index, array)
            }
            // Enums
            is Enum<*> -> {
                val pgObject = PGobject()
                pgObject.type = camelToSnakeCase(value.javaClass.simpleName)
                pgObject.value = camelToSnakeCase(value.name).uppercase()
                ps.setObject(index, pgObject)
            }

            else -> ps.setObject(index, value)
        }
    }

}