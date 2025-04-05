package org.octavius.novels.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.SaveOperation
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

                            val columnValue = when {
                                // Obsługa tablic PostgreSQL
                                columnType.startsWith("_") && listOf("_varchar", "_text", "_char").contains(columnType) -> {
                                    val array = resultSet.getArray(i)
                                    if (array != null) {
                                        (array.array as Array<*>).filterNotNull().map { it.toString() }
                                    } else null
                                }
                                // Obsługa typów enum zaczynających się od "novel_"
                                columnType.startsWith("novel_") -> {
                                    val resStr = resultSet.getString(i)
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
    fun updateDatabase(
        databaseOperations: List<SaveOperation>
    ) {
        getConnection().use { connection ->
            connection.autoCommit = false

            try {
                for (operation in databaseOperations) {
                    when (operation) {
                        is SaveOperation.Insert -> {
                            val id = insertIntoTable(connection, operation)
                            databaseOperations.filterIsInstance<SaveOperation.Insert>().forEach { op ->
                                op.foreignKeys.filter { it.referencedTable == operation.tableName }.forEach { it.value = id }
                            }
                        }

                        is SaveOperation.Update -> {
                            updateTable(connection, operation)
                        }

                        is SaveOperation.Delete -> {
                            deleteFromTable(connection, operation)
                        }
                    }
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                println("Błąd wstawiania rekordu: ${e.message}")
                throw e
            }
        }
    }

    private fun insertIntoTable(connection: Connection, operation: SaveOperation.Insert): Int? {
        val dataColumns = operation.data.keys.toList()

        // Dodaj kolumny kluczy obcych
        val foreignKeyColumns = operation.foreignKeys
            .filter { it.value != null }
            .map { it.columnName }

        val allColumns = dataColumns + foreignKeyColumns
        val columnsString = allColumns.joinToString()
        val placeholders = allColumns.joinToString { "?" }

        val insertQuery = "INSERT INTO ${operation.tableName} ($columnsString) VALUES ($placeholders) RETURNING id"

        connection.prepareStatement(insertQuery).use { statement ->
            // Ustaw parametry dla zwykłych danych
            operation.data.values.forEachIndexed { index, value ->
                setStatementParameter(statement, index + 1, value.value)
            }

            // Ustaw parametry dla kluczy obcych
            val fkValues = operation.foreignKeys
                .filter { it.value != null }
                .map { it.value }

            fkValues.forEachIndexed { index, value ->
                setStatementParameter(statement, operation.data.size + index + 1, value)
            }

            statement.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getInt(1)
                }
            }
        }

        return null
    }

    // Metoda do aktualizacji istniejącej encji
    private fun updateTable(
        connection: Connection, operation: SaveOperation.Update
    ) {
        val updateQuery = StringBuilder("UPDATE ${operation.tableName} SET ")
        val updateValues = mutableListOf<Any?>()

        operation.data.entries.forEachIndexed { index, (field, value) ->
            updateQuery.append("$field = ?")
            if (index < operation.data.size - 1) updateQuery.append(", ")
            updateValues.add(value.value)
        }

        updateQuery.append(" WHERE id = ?")
        updateValues.add(operation.id)

        connection.prepareStatement(updateQuery.toString()).use { statement ->
            updateValues.forEachIndexed { index, value ->
                setStatementParameter(statement, index + 1, value)
            }
            statement.executeUpdate()
        }
    }


    fun deleteFromTable(connection: Connection, operation: SaveOperation.Delete) {
        val deleteQuery = "DELETE FROM ${operation.tableName} WHERE id = ?"
        connection.prepareStatement(deleteQuery).use { statement ->
            setStatementParameter(statement, 1, operation.id)
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
            // Enums
            is Enum<*> -> {
                val pgObject = org.postgresql.util.PGobject()
                pgObject.type = camelToSnakeCase(value.javaClass.simpleName)
                pgObject.value = camelToSnakeCase(value.name).uppercase()
                statement.setObject(index, pgObject)
            }
            else -> statement.setObject(index, value)
        }
    }
}