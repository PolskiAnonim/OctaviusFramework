package org.octavius.novels.database

import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.util.Converters.camelToSnakeCase
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.transaction.support.TransactionTemplate
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