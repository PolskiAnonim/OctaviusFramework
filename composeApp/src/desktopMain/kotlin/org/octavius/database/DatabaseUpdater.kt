package org.octavius.database

import com.zaxxer.hikari.HikariDataSource
import org.octavius.form.SaveOperation
import org.octavius.util.Converters.camelToSnakeCase
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.transaction.support.TransactionTemplate
import java.sql.PreparedStatement
import java.sql.Types

class DatabaseUpdater(
    dataSource: HikariDataSource,
    private val jdbcTemplate: JdbcTemplate
) {
    private val transactionManager = DataSourceTransactionManager(dataSource)

    // Główna metoda do zapisywania encji
    fun updateDatabase(databaseOperations: List<SaveOperation>) {
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute { status ->
            try {
                for (operation in databaseOperations) {
                    when (operation) {
                        is SaveOperation.Insert -> {
                            val id = insertIntoTable(operation)
                            // Aktualizuj foreign keys w kolejnych operacjach
                            databaseOperations.forEach { op ->
                                when (op) {
                                    is SaveOperation.Insert -> {
                                        op.foreignKeys.filter { it.referencedTable == operation.tableName && it.value == null }
                                            .forEach { it.value = id }
                                    }

                                    is SaveOperation.Update -> {
                                        op.foreignKeys.filter { it.referencedTable == operation.tableName && it.value == null }
                                            .forEach { it.value = id }
                                    }

                                    is SaveOperation.Delete -> {
                                        op.foreignKeys.filter { it.referencedTable == operation.tableName && it.value == null }
                                            .forEach { it.value = id }
                                    }
                                }
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
                println("Błąd operacji bazodanowej: ${e.message}")
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

        val insertQuery = "INSERT INTO ${operation.tableName} ($columnsString) VALUES ($placeholders)"

        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(insertQuery, if (operation.returningId) arrayOf("id") else arrayOf())

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

        val updateQuery = if (operation.id != null) {
            "UPDATE ${operation.tableName} SET $updatePairs WHERE id = ?"
        } else {
            // Użyj foreign keys do identyfikacji wiersza
            val whereClause = operation.foreignKeys
                .filter { it.value != null }
                .joinToString(" AND ") { "${it.columnName} = ?" }
            "UPDATE ${operation.tableName} SET $updatePairs WHERE $whereClause"
        }

        jdbcTemplate.update(updateQuery) { ps ->
            // Ustaw parametry dla danych
            operation.data.values.forEachIndexed { index, value ->
                setStatementParameter(ps, index + 1, value.value)
            }

            // Ustaw parametr dla WHERE clause
            if (operation.id != null) {
                ps.setInt(operation.data.size + 1, operation.id)
            } else {
                // Ustaw parametry foreign keys
                val fkValues = operation.foreignKeys
                    .filter { it.value != null }
                    .map { it.value }

                fkValues.forEachIndexed { index, value ->
                    setStatementParameter(ps, operation.data.size + index + 1, value)
                }
            }
        }
    }

    private fun deleteFromTable(operation: SaveOperation.Delete) {
        val deleteQuery = if (operation.id != null) {
            "DELETE FROM ${operation.tableName} WHERE id = ?"
        } else {
            // Użyj foreign keys do identyfikacji wiersza
            val whereClause = operation.foreignKeys
                .filter { it.value != null }
                .joinToString(" AND ") { "${it.columnName} = ?" }
            "DELETE FROM ${operation.tableName} WHERE $whereClause"
        }
        jdbcTemplate.update(deleteQuery, { ps ->
            // Ustaw parametr dla WHERE clause
            if (operation.id != null) {
                ps.setInt(1, operation.id)
            } else {
                // Ustaw parametry foreign keys
                val fkValues = operation.foreignKeys
                    .filter { it.value != null }
                    .map { it.value }

                fkValues.forEachIndexed { index, value ->
                    setStatementParameter(ps, index + 1, value)
                }
            }
        })
    }

    // Pomocnicza metoda do ustawiania parametrów w PreparedStatement
    private fun setStatementParameter(ps: PreparedStatement, index: Int, value: Any?) {
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