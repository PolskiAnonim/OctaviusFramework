package org.octavius.database

import com.zaxxer.hikari.HikariDataSource
import org.octavius.form.SaveOperation
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.transaction.support.TransactionTemplate

class DatabaseUpdater(
    dataSource: HikariDataSource,
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    val typesConverter: DatabaseToKotlinTypesConverter
) {
    private val transactionManager = DataSourceTransactionManager(dataSource)
    private val parameterExpandHelper = ParameterExpandHelper()

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
        val placeholders = allColumns.joinToString { ":$it" }

        val insertQuery = "INSERT INTO ${operation.tableName} ($columnsString) VALUES ($placeholders)"

        val keyHolder = GeneratedKeyHolder()

        // Przygotuj mapę parametrów
        val params = mutableMapOf<String, Any?>()

        // Dodaj parametry dla zwykłych danych
        operation.data.forEach { (key, value) ->
            params[key] = value.value
        }

        // Dodaj parametry dla kluczy obcych
        operation.foreignKeys
            .filter { it.value != null }
            .forEach { fk ->
                params[fk.columnName] = fk.value
            }

        val expanded = parameterExpandHelper.expandParametersInQuery(insertQuery, params)
        namedParameterJdbcTemplate.update(expanded.expandedSql, MapSqlParameterSource(expanded.expandedParams), keyHolder, if (operation.returningId) arrayOf("id") else arrayOf())

        return keyHolder.keys?.get("id") as? Int
    }

    private fun updateTable(operation: SaveOperation.Update) {
        val updatePairs = operation.data.entries.joinToString { "${it.key} = :${it.key}" }

        val updateQuery = if (operation.id != null) {
            "UPDATE ${operation.tableName} SET $updatePairs WHERE id = :id"
        } else {
            // Użyj foreign keys do identyfikacji wiersza
            val whereClause = operation.foreignKeys
                .filter { it.value != null }
                .joinToString(" AND ") { "${it.columnName} = :${it.columnName}" }
            "UPDATE ${operation.tableName} SET $updatePairs WHERE $whereClause"
        }

        // Przygotuj mapę parametrów
        val params = mutableMapOf<String, Any?>()

        // Dodaj parametry dla danych do zaktualizowania
        operation.data.forEach { (key, value) ->
            params[key] = value.value
        }

        // Dodaj parametr dla WHERE clause
        if (operation.id != null) {
            params["id"] = operation.id
        } else {
            // Dodaj parametry foreign keys
            operation.foreignKeys
                .filter { it.value != null }
                .forEach { fk ->
                    params[fk.columnName] = fk.value
                }
        }

        val expanded = parameterExpandHelper.expandParametersInQuery(updateQuery, params)
        namedParameterJdbcTemplate.update(expanded.expandedSql, expanded.expandedParams)
    }

    private fun deleteFromTable(operation: SaveOperation.Delete) {
        val deleteQuery = if (operation.id != null) {
            "DELETE FROM ${operation.tableName} WHERE id = :id"
        } else {
            // Użyj foreign keys do identyfikacji wiersza
            val whereClause = operation.foreignKeys
                .filter { it.value != null }
                .joinToString(" AND ") { "${it.columnName} = :${it.columnName}" }
            "DELETE FROM ${operation.tableName} WHERE $whereClause"
        }

        // Przygotuj mapę parametrów
        val params = mutableMapOf<String, Any?>()

        // Dodaj parametr dla WHERE clause
        if (operation.id != null) {
            params["id"] = operation.id
        } else {
            // Dodaj parametry foreign keys
            operation.foreignKeys
                .filter { it.value != null }
                .forEach { fk ->
                    params[fk.columnName] = fk.value
                }
        }

        val expanded = parameterExpandHelper.expandParametersInQuery(deleteQuery, params)
        namedParameterJdbcTemplate.update(expanded.expandedSql, expanded.expandedParams)
    }

    // Prosta metoda do wykonywania SQL z nazwanymi parametrami
    fun executeUpdate(sql: String, params: Map<String, Any?> = emptyMap()): Int {
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)
        return namedParameterJdbcTemplate.update(expanded.expandedSql, expanded.expandedParams)
    }
}