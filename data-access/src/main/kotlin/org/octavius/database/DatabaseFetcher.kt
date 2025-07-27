package org.octavius.database

import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Klasa odpowiedzialna za pobieranie danych z bazy PostgreSQL.
 *
 * Zapewnia zaawansowane metody do wykonywania zapytań SELECT z obsługą:
 * - Filtrowania, sortowania i paginacji
 * - Złożonych wyrażeń tabelowych (JOIN, subqueries)
 * - Automatycznej ekspansji parametrów (array, enum, composite types)
 * - Konwersji typów PostgreSQL na typy Kotlin
 * - Mapowania kolumn na obiekty ColumnInfo
 */

/**
 * Główna klasa do pobierania danych z bazy z zaawansowaną obsługą typów.
 *
 * @param jdbcTemplate Template Spring JDBC do wykonywania zapytań
 * @param rowMappers Fabryka mapperów do konwersji wyników na obiekty Kotlin
 *
 * Przykład użycia:
 * ```kotlin
 * val fetcher = DatabaseFetcher(jdbcTemplate, rowMappers)
 * val count = fetcher.fetchCount("users", "age > :age", mapOf("age" to 18))
 * val users = fetcher.fetchPagedList("users", "*", 0, 10, "active = true")
 * ```
 */
class DatabaseFetcher(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    val rowMappers: RowMappers,
    private val parameterExpandHelper: ParameterExpandHelper
) : DataFetcher {
    /**
     * Formatuje wyrażenie tabelowe dla bezpiecznego użycia w SQL.
     *
     * Automatycznie dodaje nawiasy dla złożonych wyrażeń (JOIN, subqueries).
     *
     * @param table Nazwa tabeli lub złożone wyrażenie SQL
     * @return Sformatowane wyrażenie gotowe do użycia w zapytaniu
     */
    private fun formatTableExpression(table: String): String {
        return if (table.trim().uppercase().contains(" ")) "($table)" else table
    }

    /**
     * Pobiera liczbę wierszy z tabeli z opcjonalnym filtrowaniem.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param filter Opcjonalne wyrażenie WHERE (bez słowa kluczowego WHERE)
     * @param params Parametry do podstawienia w zapytaniu
     * @return Liczba wierszy spełniających warunki
     *
     * Przykład:
     * ```kotlin
     * val count = fetchCount("users", "age > :minAge AND status = :status",
     *                       mapOf("minAge" to 18, "status" to "active"))
     * ```
     */
    override fun fetchCount(table: String, filter: String?, params: Map<String, Any?>): Long {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT COUNT(*) AS count FROM ${formatTableExpression(table)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, Long::class.java) ?: 0L
    }


    /**
     * Pobiera wartość pojedynczego pola z pierwszego wiersza.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param field Nazwa pola do pobrania
     * @param filter Opcjonalne wyrażenie WHERE
     * @param params Parametry do podstawienia
     * @return Wartość pola
     *
     * @throws DataAccessException gdy zapytanie nie zwróci wyników lub zwróci ponad jeden wiersz
     */
    override fun fetchField(table: String, field: String, filter: String?, params: Map<String, Any?>): Any? {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT $field FROM ${formatTableExpression(table)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(
            expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper()
        )
    }

    /**
     * Pobiera pojedynczy wiersz z bazy danych.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param columns Lista pól do pobrania (np. "id, name, email")
     * @param filter Opcjonalne wyrażenie WHERE
     * @param params Parametry do podstawienia
     * @return Mapa kolumn do wartości
     *
     * @throws IllegalStateException gdy zapytanie zwróci więcej niż 1 wiersz
     * @throws NullPointerException gdy nie znaleziono wiersza
     */
    override fun fetchRow(
        table: String,
        columns: String,
        filter: String?,
        params: Map<String, Any?>
    ): Map<String, Any?> {
        val results = fetchRowOrNull(table, columns, filter, params)
        return results!!
    }

    /**
     * Pobiera pojedynczy wiersz lub null jeśli nie znaleziono.
     *
     * Wersja fetchRow która zwraca null w przypadku braku dopasowania.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param columns Lista pól do pobrania
     * @param filter Opcjonalne wyrażenie WHERE
     * @param params Parametry do podstawienia
     * @return Mapa kolumn do wartości lub null jeśli nie znaleziono
     *
     * @throws IllegalStateException gdy zapytanie zwróci więcej niż 1 wiersz
     */
    override fun fetchRowOrNull(
        table: String, columns: String, filter: String?, params: Map<String, Any?>
    ): Map<String, Any?>? {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT $columns FROM ${formatTableExpression(table)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)
        val results = jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())

        return when (results.size) {
            0 -> null
            1 -> results[0]
            else -> throw IllegalStateException("Query returned ${results.size} rows, expected 0 or 1")
        }
    }

    /**
     * Pobiera wartości z pojedynczej kolumny.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param column Nazwa kolumny do pobrania
     * @param filter Opcjonalne wyrażenie WHERE
     * @param orderBy Opcjonalne wyrażenie ORDER BY (bez słowa kluczowego)
     * @param params Parametry do podstawienia
     * @return Lista wartości z kolumny
     */
    override fun fetchColumn(
        table: String,
        column: String,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $column FROM ${formatTableExpression(table)}$whereClause$orderClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    /**
     * Pobiera wartości z kolumny z paginacją.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param column Nazwa kolumny do pobrania
     * @param offset Liczba wierszy do pominięcia
     * @param limit Maksymalna liczba wierszy do pobrania
     * @param filter Opcjonalne wyrażenie WHERE
     * @param orderBy Opcjonalne wyrażenie ORDER BY
     * @param params Parametry do podstawienia
     * @return Lista wartości z kolumny (maksymalnie `limit` elementów)
     */
    override fun fetchPagedColumn(
        table: String,
        column: String,
        offset: Int,
        limit: Int,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql =
            "SELECT $column FROM ${formatTableExpression(table)}$whereClause$orderClause LIMIT $limit OFFSET $offset"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.SingleValueMapper())
    }

    /**
     * Pobiera listę wierszy z bazy danych.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param columns Lista pól do pobrania (np. "id, name, email")
     * @param filter Opcjonalne wyrażenie WHERE
     * @param orderBy Opcjonalne wyrażenie ORDER BY
     * @param params Parametry do podstawienia
     * @return Lista map reprezentujących wiersze
     */
    override fun fetchList(
        table: String,
        columns: String,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Map<String, Any?>> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql = "SELECT $columns FROM ${formatTableExpression(table)}$whereClause$orderClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())
    }

    /**
     * Pobiera listę wierszy z paginacją.
     *
     * Główna metoda do pobierania danych dla tabel z raportami.
     *
     * @param table Nazwa tabeli lub wyrażenie tabelowe
     * @param columns Lista pól do pobrania
     * @param offset Liczba wierszy do pominięcia (dla paginacji)
     * @param limit Maksymalna liczba wierszy do pobrania
     * @param filter Opcjonalne wyrażenie WHERE
     * @param orderBy Opcjonalne wyrażenie ORDER BY
     * @param params Parametry do podstawienia
     * @return Lista map reprezentujących wiersze (maksymalnie `limit` elementów)
     */
    override fun fetchPagedList(
        table: String,
        columns: String,
        offset: Int,
        limit: Int,
        filter: String?,
        orderBy: String?,
        params: Map<String, Any?>
    ): List<Map<String, Any?>> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val orderClause = if (!orderBy.isNullOrBlank()) " ORDER BY $orderBy" else ""
        val sql =
            "SELECT $columns FROM ${formatTableExpression(table)}$whereClause$orderClause LIMIT $limit OFFSET $offset"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.query(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnNameMapper())
    }

    /**
     * Pobiera encję z pełnymi informacjami o kolumnach.
     *
     * Używa ColumnInfoMapper do mapowania wyników na obiekty ColumnInfo
     * zawierające nazwę kolumny i tabeli z której pochodzą informacje.
     *
     * Używane przede wszystkim w formularzach. Wymaga filtra który wymusi zwrócenie jednego wiersza
     *
     * @param tables Wyrażenie tabelowe (może zawierać JOIN)
     * @param filter Opcjonalne wyrażenie WHERE
     * @param params Parametry do podstawienia
     * @return Mapa obiektów ColumnInfo do wartości
     *
     * Przykład:
     * ```kotlin
     * val entity = fetchEntity(
     *     "users u LEFT JOIN profiles p ON u.id = p.user_id",
     *     "u.id = :id",
     *     mapOf("id" to 123)
     * )
     * ```
     */
    override fun fetchEntity(
        tables: String,
        filter: String?,
        params: Map<String, Any?>
    ): Map<ColumnInfo, Any?> {
        val whereClause = if (!filter.isNullOrBlank()) " WHERE $filter" else ""
        val sql = "SELECT * FROM ${formatTableExpression(tables)}$whereClause"
        val expanded = parameterExpandHelper.expandParametersInQuery(sql, params)

        return jdbcTemplate.queryForObject(expanded.expandedSql, expanded.expandedParams, rowMappers.ColumnInfoMapper())
            ?: emptyMap()
    }
}