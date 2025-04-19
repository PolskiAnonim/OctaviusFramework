package org.octavius.novels.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation
import org.octavius.novels.util.Converters.camelToSnakeCase
import org.octavius.novels.util.Converters.snakeToCamelCase
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.queryForObject
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object DatabaseManager {
    private val dataSource: HikariDataSource
    private const val jdbcUrl = "jdbc:postgresql://localhost:5430/novels_games"
    private const val username = "postgres"
    private const val password = "1234"
    private val transactionManager: DataSourceTransactionManager
    private val jdbcTemplate: JdbcTemplate

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@DatabaseManager.jdbcUrl
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)
        jdbcTemplate = JdbcTemplate(dataSource)
        transactionManager = DataSourceTransactionManager(dataSource)
    }

    private fun getConnection(): Connection = dataSource.connection

    fun <T : Any> getDataForPage(
        tableName: String,
        currentPage: Int,
        pageSize: Int,
        whereClause: String,
        resultClass: KClass<T>
    ): Pair<List<T>, Long> {
        var totalElements: Long;
        val offset = (currentPage - 1) * pageSize;
        val results = mutableListOf<T>()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) AS total FROM $tableName $whereClause").use { resultSet ->
                    resultSet.next()
                    totalElements = resultSet.getLong("total")
                }
                val sql = "SELECT * FROM $tableName $whereClause  ORDER BY id LIMIT $pageSize OFFSET $offset"
                statement.executeQuery(sql).use { resultSet ->
                    val constructor = resultClass.primaryConstructor!!
                    val parameters = constructor.parameters

                    while (resultSet.next()) {
                        val args = parameters.associateWith { param ->
                            val columnName = param.name!!
                            when (param.type.classifier) {
                                String::class -> resultSet.getString(camelToSnakeCase(columnName))
                                Int::class -> resultSet.getInt(camelToSnakeCase(columnName))
                                Long::class -> resultSet.getLong(camelToSnakeCase(columnName))
                                Double::class -> resultSet.getDouble(camelToSnakeCase(columnName))
                                Boolean::class -> resultSet.getBoolean(camelToSnakeCase(columnName))
                                NovelStatus::class -> NovelStatus.valueOf(
                                    resultSet.getString(
                                        camelToSnakeCase(
                                            columnName
                                        )
                                    )
                                )

                                List::class -> {
                                    val array = resultSet.getArray(camelToSnakeCase(columnName))
                                    when (val arrayContent = array.array) {
                                        is Array<*> -> arrayContent.toList()
                                        else -> throw IllegalArgumentException("Unexpected array type: ${arrayContent?.javaClass}")
                                    }
                                }

                                else -> throw IllegalArgumentException("Unsupported type: ${param.type}")
                            }
                        }

                        results.add(constructor.callBy(args))
                    }
                }
            }
        }
        return Pair<List<T>, Long>(results, totalElements)
    }

    //----------------------------------------------formularze----------------------------------------------------------
    // pobieranie encji
    fun getEntityWithRelations(
        id: Int,
        tableRelations: List<TableRelation>
    ): Map<ColumnInfo, Any?> {
        if (tableRelations.isEmpty()) {
            throw IllegalArgumentException("Lista relacji tabel nie może być pusta")
        }

        val mainTable = tableRelations.first().tableName
        val result = mutableMapOf<ColumnInfo, Any?>()

        // Budowanie zapytania SQL z wieloma JOIN-ami
        val sqlBuilder = StringBuilder("SELECT * FROM $mainTable ")

        // Dodawanie JOIN-ów
        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            sqlBuilder.append("LEFT JOIN ${relation.tableName} ON ${relation.joinCondition} ")
        }

        sqlBuilder.append("WHERE $mainTable.id = ?")

        // Definiujemy mapper jako klasę wewnętrzną
        class EntityRowMapper : RowMapper<Map<ColumnInfo, Any?>> {
            override fun mapRow(rs: ResultSet, rowNum: Int): Map<ColumnInfo, Any?> {
                val rowResult = mutableMapOf<ColumnInfo, Any?>()
                val metaData = rs.metaData

                for (i in 1..metaData.columnCount) {
                    val columnName = metaData.getColumnName(i)
                    val tableName = metaData.getTableName(i)
                    val columnType = metaData.getColumnTypeName(i)

                    val columnValue = when {
                        // Obsługa tablic PostgreSQL
                        columnType.startsWith("_") && listOf("_varchar", "_text", "_char").contains(columnType) -> {
                            val array = rs.getArray(i)
                            if (array != null) {
                                (array.array as Array<*>).filterNotNull().map { it.toString() }
                            } else null
                        }
                        // Obsługa typów enum zaczynających się od "novel_"
                        columnType.startsWith("novel_") -> {
                            val resStr = rs.getString(i)
                            if (resStr != null) {
                                // Konwersja z formatu PostgreSQL do Kotlin
                                val enumClassName = snakeToCamelCase(columnType, true)

                                try {
                                    // Dynamiczne pobranie klasy enuma i wywołanie valueOf
                                    val enumClass = Class.forName("org.octavius.novels.domain.$enumClassName")
                                    val valueOfMethod = enumClass.getMethod("valueOf", String::class.java)
                                    val normalizedValue = snakeToCamelCase(resStr, true)
                                    valueOfMethod.invoke(null, normalizedValue)
                                } catch (e: Exception) {
                                    println("Nie można przekonwertować wartości enum: $resStr dla typu $columnType")
                                    e.printStackTrace()
                                    null
                                }
                            } else null
                        }
                        // Standardowa obsługa pozostałych typów
                        else -> rs.getObject(i)
                    }

                    rowResult[ColumnInfo(tableName, columnName)] = columnValue
                }

                return rowResult
            }
        }

        // Wykonanie zapytania z mapperem
        return jdbcTemplate.queryForObject(
            sqlBuilder.toString(),
            EntityRowMapper(),
            id
        )!!
    }

    //------------------------------------zapis

    // Główna metoda do zapisu/aktualizacji encji
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

        // Przygotowanie parametrów
        val params = mutableListOf<Any?>()

        // Wartości dla zwykłych danych
        operation.data.values.forEach { value ->
            params.add(convertValueForJdbc(value.value))
        }

        // Wartości dla kluczy obcych
        operation.foreignKeys
            .filter { it.value != null }
            .forEach { params.add(it.value) }

        // Wykonanie zapytania i pobranie wygenerowanego ID
        return jdbcTemplate.queryForObject(insertQuery, Int::class.java, *params.toTypedArray())
    }

    private fun updateTable(operation: SaveOperation.Update) {
        val updateQuery = StringBuilder("UPDATE ${operation.tableName} SET ")
        val params = mutableListOf<Any?>()

        operation.data.entries.forEachIndexed { index, (field, value) ->
            updateQuery.append("$field = ?")
            if (index < operation.data.size - 1) updateQuery.append(", ")
            params.add(convertValueForJdbc(value.value))
        }

        updateQuery.append(" WHERE id = ?")
        params.add(operation.id)

        jdbcTemplate.update(updateQuery.toString(), *params.toTypedArray())
    }

    private fun deleteFromTable(operation: SaveOperation.Delete) {
        jdbcTemplate.update("DELETE FROM ${operation.tableName} WHERE id = ?", operation.id)
    }

    // Pomocnicza metoda do konwersji wartości dla JDBC
    private fun convertValueForJdbc(value: Any?): Any? {
        return when (value) {
            null -> null
            is List<*> -> {
                // Dla PostgreSQL, konwertujemy listy na tablice
                val array = jdbcTemplate.dataSource!!.connection.use { conn ->
                    conn.createArrayOf("text", value.toTypedArray())
                }
                array
            }
            is Enum<*> -> {
                // Dla enum tworzymy PGobject
                val pgObject = org.postgresql.util.PGobject()
                pgObject.type = camelToSnakeCase(value.javaClass.simpleName)
                pgObject.value = camelToSnakeCase(value.name).uppercase()
                pgObject
            }
            else -> value
        }
    }
}