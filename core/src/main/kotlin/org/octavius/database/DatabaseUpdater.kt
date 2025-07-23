package org.octavius.database

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.transaction.support.TransactionTemplate

/**
 * Klasa odpowiedzialna za wykonywanie operacji modyfikujących bazę danych.
 * 
 * Zapewnia zaawansowane funkcje do operacji INSERT/UPDATE/DELETE z obsługą:
 * - Transakcyjności (wszystkie operacje lub żadna)
 * - Zarządzania kluczami obcymi między operacjami
 * - Automatycznego generowania kluczy głównych
 * - Ekspansji złożonych parametrów PostgreSQL
 * - Rollback przy błędach z pełnym komunikatem
 */

/**
 * Komponent do wykonywania transakcyjnych operacji modyfikujących.
 * 
 * @param transactionManager Menedżer transakcji Spring do obsługi ACID
 * @param namedParameterJdbcTemplate Template JDBC z obsługą named parameters
 * 
 * Obsługuje:
 * - INSERT z automatycznym generowaniem ID i obsługą foreign keys
 * - UPDATE z identyfikacją przez ID lub foreign keys
 * - DELETE z identyfikacją przez ID lub foreign keys
 * - Zarządzanie zależnościami między operacjami w ramach transakcji
 * 
 * Przykład użycia:
 * ```kotlin
 * val operations = listOf(
 *     SaveOperation.Insert("users", userData, returningId = true),
 *     SaveOperation.Update("profiles", profileData, id = profileId)
 * )
 * updater.updateDatabase(operations)
 * ```
 */
class DatabaseUpdater(
    val transactionManager: DataSourceTransactionManager,
    val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {
    /** Helper do ekspansji złożonych parametrów PostgreSQL */
    private val parameterExpandHelper = ParameterExpandHelper()

    /**
     * Główna metoda wykonująca listę operacji w pojedynczej transakcji.
     * 
     * Operacje są wykonywane sekwencyjnie w podanej kolejności.
     * Klucze główne wygenerowane przez operacje INSERT są automatycznie
     * przekazywane do kolejnych operacji jako foreign keys.
     * 
     * @param databaseOperations Lista operacji SaveOperation do wykonania
     * 
     * @throws Exception gdy którakolwiek operacja się nie powiedzie (z rollback)
     * 
     * Przykład z zależnościami:
     * ```kotlin
     * updateDatabase(listOf(
     *     SaveOperation.Insert("users", userData, returningId = true),
     *     SaveOperation.Insert("profiles", profileData, foreignKeys = listOf(
     *         ForeignKeyReference("user_id", "users", null) // zostanie wypełnione automatycznie
     *     ))
     * ))
     * ```
     */
    fun updateDatabase(databaseOperations: List<SaveOperation>) {
        val transactionTemplate = TransactionTemplate(transactionManager)

        transactionTemplate.execute { status ->
            try {
                for ((index, operation) in databaseOperations.withIndex()) {
                    when (operation) {
                        is SaveOperation.Insert -> {
                            val id = insertIntoTable(operation)
                            // Aktualizuj foreign keys w kolejnych operacjach, ale przerwij gdy napotkasz kolejny INSERT do tej samej tabeli
                            for (i in (index + 1) until databaseOperations.size) {
                                val op = databaseOperations[i]
                                
                                // Przerwij jeśli napotkasz kolejny INSERT do tej samej tabeli
                                if (op is SaveOperation.Insert && op.tableName == operation.tableName) {
                                    break
                                }

                                op.foreignKeys.filter { it.referencedTable == operation.tableName && it.value == null }
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

    /**
     * Wykonuje operację INSERT z obsługą foreign keys i generowania ID.
     * 
     * @param operation Operacja INSERT do wykonania
     * @return Wygenerowane ID (jeśli returningId = true) lub null
     * 
     * Funkcjonalności:
     * - Automatyczne generowanie placeholders dla wszystkich kolumn
     * - Obsługa foreign keys (tylko te z wartością != null)
     * - Generowanie klucza głównego gdy returningId = true
     * - Ekspansja złożonych parametrów (arrays, enums, composite types)
     */
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
            params[key] = value
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

    /**
     * Wykonuje operację UPDATE z elastyczną identyfikacją wiersza.
     * 
     * @param operation Operacja UPDATE do wykonania
     * 
     * Identyfikacja wiersza:
     * - Jeśli podano ID: WHERE id = :id
     * - Jeśli nie ma ID: WHERE na podstawie foreign keys
     * 
     * Przykład UPDATE przez foreign keys:
     * ```kotlin
     * SaveOperation.Update(
     *     "user_preferences",
     *     data = mapOf("theme" to FormValue("dark")),
     *     foreignKeys = listOf(
     *         ForeignKeyReference("user_id", "users", 123)
     *     )
     * )
     * ```
     */
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
            params[key] = value
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

    /**
     * Wykonuje operację DELETE z elastyczną identyfikacją wiersza.
     * 
     * @param operation Operacja DELETE do wykonania
     * 
     * Identyfikacja wiersza:
     * - Jeśli podano ID: WHERE id = :id
     * - Jeśli nie ma ID: WHERE na podstawie foreign keys
     * 
     * UWAGA: Upewnij się, że warunki WHERE są wystarczająco specyficzne!
     */
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

    /**
     * Wykonuje dowolne zapytanie SQL modyfikujące z named parameters.
     * 
     * Metoda pomocnicza do prostych operacji UPDATE/INSERT/DELETE
     * które nie wymagają zaawansowanej logiki SaveOperation.
     * 
     * @param sql Zapytanie SQL z named parameters (np. ":param")
     * @param params Mapa parametrów do podstawienia
     * @return Liczba zmodyfikowanych wierszy
     * 
     * Przykład:
     * ```kotlin
     * val affected = executeUpdate(
     *     "UPDATE users SET last_login = :time WHERE id = :id",
     *     mapOf("time" to Instant.now(), "id" to 123)
     * )
     * ```
     */
    fun executeUpdate(sql: String, params: Map<String, Any?> = emptyMap()): Int {
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)
        return namedParameterJdbcTemplate.update(expanded.expandedSql, expanded.expandedParams)
    }
    
    /**
     * Wykonuje zapytanie SQL z klauzulą RETURNING.
     * 
     * Użyteczne dla operacji INSERT/UPDATE które potrzebują zwrócić
     * wartość wygenerowanej kolumny.
     * 
     * @param sql Zapytanie SQL z RETURNING lub bez (zostanie użyty GeneratedKeyHolder)
     * @param params Mapa parametrów do podstawienia
     * @param columnName Nazwa kolumny do zwrócenia
     * @return Wartość zwracanej kolumny lub null
     * 
     * Przykład:
     * ```kotlin
     * val newId = executeReturning(
     *     "INSERT INTO logs (message, created_at) VALUES (:msg, NOW())",
     *     mapOf("msg" to "User logged in"),
     *     "id"
     * ) as Int
     * ```
     */
    fun executeReturning(sql: String, params: Map<String, Any?> = emptyMap(), columnName: String): Any? {
        val keyHolder = GeneratedKeyHolder()
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)
        namedParameterJdbcTemplate.update(expanded.expandedSql, MapSqlParameterSource(expanded.expandedParams), keyHolder, arrayOf(columnName))
        return keyHolder.keys?.get(columnName)
    }
}