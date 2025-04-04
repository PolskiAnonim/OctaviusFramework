package org.octavius.novels.database

import androidx.compose.runtime.compositionLocalOf
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.TableRelation
import org.octavius.novels.util.Converters.camelToSnakeCase
import org.octavius.novels.util.Converters.snakeToCamelCase
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object DatabaseManager {
    private val dataSource: HikariDataSource
    private const val jdbcUrl = "jdbc:postgresql://localhost:5430/novels_games"
    private const val username = "postgres"
    private const val password = "1234"

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@DatabaseManager.jdbcUrl
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)
    }

    private fun getConnection(): Connection = dataSource.connection

    fun <T: Any> getDataForPage(tableName: String, currentPage: Int, pageSize: Int, whereClause: String, resultClass: KClass<T>): Pair<List<T>, Long> {
        var totalElements: Long;
        val offset = (currentPage - 1) * pageSize;
        val results = mutableListOf<T>()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) AS total FROM $tableName $whereClause").use { resultSet ->
                    resultSet.next()
                    totalElements = resultSet.getLong("total")
                }
                val sql = "SELECT * FROM $tableName $whereClause LIMIT $pageSize OFFSET $offset"
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
                                NovelStatus::class -> NovelStatus.valueOf(resultSet.getString(camelToSnakeCase(columnName)))
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
        // Tabela główna
        val sqlBuilder = StringBuilder("SELECT * FROM $mainTable ")

        // Dodawanie JOIN-ów
        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            sqlBuilder.append("LEFT JOIN ${relation.tableName} ON ${relation.joinCondition} ")
        }

        sqlBuilder.append("WHERE $mainTable.id = ?")

        // pobranie danych
        getConnection().use { connection ->
            connection.prepareStatement(sqlBuilder.toString()).use { statement ->
                statement.setInt(1, id)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        val metaData = resultSet.metaData
                        for (i in 1..metaData.columnCount) {
                            val columnName = metaData.getColumnName(i)
                            val tableName = metaData.getTableName(i)
                            val columnType = metaData.getColumnTypeName(i)

                            val columnValue = when (columnType) {
                                // Obsługa tablic PostgreSQL
                                "_varchar", "_text", "_char" -> {
                                    val array = resultSet.getArray(i)
                                    if (array != null) {
                                        (array.array as Array<*>).filterNotNull().map { it.toString() }
                                    } else null
                                }
                                // Obsługa typów enum
                                "novel_status" -> {
                                    val statusStr = resultSet.getString(i)
                                    if (statusStr != null) {
                                        // Konwersja z formatu PostgreSQL do Kotlin
                                        val normalizedStatus = snakeToCamelCase(statusStr)
                                        try {
                                            NovelStatus.valueOf(normalizedStatus)
                                        } catch (e: IllegalArgumentException) {
                                            println("Nie można przekonwertować wartości enum: $statusStr")
                                            null
                                        }
                                    } else null
                                }
                                // Standardowa obsługa pozostałych typów
                                else -> resultSet.getObject(i)
                            }

                            result[ColumnInfo(tableName, columnName)] = columnValue
                        }
                    }
                }
            }
        }

        return result
    }

    //------------------------------------zapis

    // Główna metoda do zapisu/aktualizacji encji
    fun saveOrUpdateEntity(
        mainTable: String,
        mainData: Map<String, Any?>,
        relatedData: Map<String, Pair<String, Map<String, Any?>>>
    ): Int? {
        val id = mainData["id"] as? Int

        return if (id != null) {
            updateEntity(id, mainTable, mainData, relatedData)
            id
        } else {
            insertEntity(mainTable, mainData, relatedData)
        }
    }

    // Metoda do aktualizacji istniejącej encji
    private fun updateEntity(
        id: Int,
        mainTable: String,
        mainData: Map<String, Any?>,
        relatedData: Map<String, Pair<String, Map<String, Any?>>>
    ): Boolean {
        getConnection().use { connection ->
            connection.autoCommit = false

            try {
                // Aktualizacja głównej tabeli
                val filteredMainData = mainData.filterKeys { it != "id" }
                if (filteredMainData.isNotEmpty()) {
                    val updateQuery = StringBuilder("UPDATE $mainTable SET ")
                    val updateValues = mutableListOf<Any?>()

                    filteredMainData.entries.forEachIndexed { index, (field, value) ->
                        updateQuery.append("$field = ?")
                        if (index < filteredMainData.size - 1) updateQuery.append(", ")
                        updateValues.add(value)
                    }

                    updateQuery.append(" WHERE id = ?")
                    updateValues.add(id)

                    connection.prepareStatement(updateQuery.toString()).use { statement ->
                        updateValues.forEachIndexed { index, value ->
                            setStatementParameter(statement, index + 1, value)
                        }
                        statement.executeUpdate()
                    }
                }

                // Aktualizacja powiązanych tabel
                for ((tableName, data) in relatedData) {
                    val (joinCondition, values) = data
                    updateRelatedTable(connection, tableName, values, id, joinCondition)
                }

                connection.commit()
                return true
            } catch (e: Exception) {
                connection.rollback()
                println("Błąd aktualizacji rekordu: ${e.message}")
                throw e
            }
        }
    }

    // Metoda do wstawiania nowej encji
    private fun insertEntity(
        mainTable: String,
        mainData: Map<String, Any?>,
        relatedData: Map<String, Pair<String, Map<String, Any?>>>
    ): Int? {
        getConnection().use { connection ->
            connection.autoCommit = false

            try {
                // Wstawianie do głównej tabeli
                val filteredMainData = mainData.filterKeys { it != "id" }
                var newId: Int? = null

                if (filteredMainData.isNotEmpty()) {
                    val columns = filteredMainData.keys.joinToString(", ")
                    val placeholders = filteredMainData.keys.map { "?" }.joinToString(", ")

                    val insertQuery = "INSERT INTO $mainTable ($columns) VALUES ($placeholders) RETURNING id"

                    connection.prepareStatement(insertQuery).use { statement ->
                        filteredMainData.values.forEachIndexed { index, value ->
                            setStatementParameter(statement, index + 1, value)
                        }

                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                newId = resultSet.getInt("id")
                            }
                        }
                    }
                }

                if (newId == null) {
                    throw Exception("Nie udało się wstawić głównego rekordu")
                }

                // Wstawianie do powiązanych tabel
                for ((tableName, data) in relatedData) {
                    val (joinCondition, values) = data
                    insertRelatedTable(connection, tableName, values, newId, joinCondition)
                }

                connection.commit()
                return newId
            } catch (e: Exception) {
                connection.rollback()
                println("Błąd wstawiania rekordu: ${e.message}")
                throw e
            }
        }
    }

    // Aktualizacja powiązanej tabeli
    private fun updateRelatedTable(
        connection: Connection,
        tableName: String,
        data: Map<String, Any?>,
        mainId: Int,
        joinCondition: String
    ) {
        // Parsowanie warunku JOIN, aby wyciągnąć klucz obcy
        val joinParts = joinCondition.split("=").map { it.trim() }
        if (joinParts.size != 2) return

        val foreignKeyColumn = joinParts[1].split(".").lastOrNull()?.trim() ?: return

        // Sprawdź, czy rekord już istnieje
        var exists = false
        connection.prepareStatement("SELECT 1 FROM $tableName WHERE $foreignKeyColumn = ?").use { statement ->
            statement.setInt(1, mainId)
            statement.executeQuery().use { resultSet ->
                exists = resultSet.next()
            }
        }

        if (exists) {
            // Aktualizuj istniejący rekord
            val updateQuery = StringBuilder("UPDATE $tableName SET ")
            val updateValues = mutableListOf<Any?>()

            data.entries.forEachIndexed { index, (field, value) ->
                updateQuery.append("$field = ?")
                if (index < data.size - 1) updateQuery.append(", ")
                updateValues.add(value)
            }

            updateQuery.append(" WHERE $foreignKeyColumn = ?")
            updateValues.add(mainId)

            connection.prepareStatement(updateQuery.toString()).use { statement ->
                updateValues.forEachIndexed { index, value ->
                    setStatementParameter(statement, index + 1, value)
                }
                statement.executeUpdate()
            }
        } else {
            // Wstaw nowy rekord
            insertRelatedTable(connection, tableName, data, mainId, joinCondition)
        }
    }

    // Wstawianie do powiązanej tabeli
    private fun insertRelatedTable(
        connection: Connection,
        tableName: String,
        data: Map<String, Any?>,
        mainId: Int,
        joinCondition: String
    ) {
        // Parsowanie warunku JOIN, aby wyciągnąć klucz obcy
        val joinParts = joinCondition.split("=").map { it.trim() }
        if (joinParts.size != 2) return

        val foreignKeyColumn = joinParts[1].split(".").lastOrNull()?.trim() ?: return

        // Przygotuj dane do wstawienia
        val fieldsWithFk = data.toMutableMap()
        fieldsWithFk[foreignKeyColumn] = mainId

        val columns = fieldsWithFk.keys.joinToString(", ")
        val placeholders = fieldsWithFk.keys.map { "?" }.joinToString(", ")

        val insertQuery = "INSERT INTO $tableName ($columns) VALUES ($placeholders)"

        connection.prepareStatement(insertQuery).use { statement ->
            fieldsWithFk.values.forEachIndexed { index, value ->
                setStatementParameter(statement, index + 1, value)
            }
            statement.executeUpdate()
        }
    }

    // Ustawianie parametrów w zapytaniu SQL
    private fun setStatementParameter(statement: PreparedStatement, index: Int, value: Any?) {
        when (value) {
            null -> statement.setNull(index, Types.NULL)
            is Int -> statement.setInt(index, value)
            is Long -> statement.setLong(index, value)
            is String -> statement.setString(index, value)
            is Boolean -> statement.setBoolean(index, value)
            is Double -> statement.setDouble(index, value)
            is List<*> -> {
                // Dla list, konwertujemy na tablicę
                val array = statement.connection.createArrayOf("text", value.toTypedArray())
                statement.setArray(index, array)
            }
            else -> statement.setObject(index, value)
        }
    }
}