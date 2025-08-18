package org.octavius.data.contract


import kotlin.reflect.KClass

/**
 * Kontrakt dla operacji pobierania danych (SELECT).
 *
 * Uniezależnia logikę biznesową od konkretnej implementacji dostępu do danych.
 */
interface DataFetcher {
    /**
     * Rozpoczyna proces budowania zapytania SELECT. Jest to główny punkt wejścia
     * do pobierania danych w formie list i pojedynczych wierszy.
     *
     * @param columns Lista kolumn do pobrania, np. "id, name, email". Domyślnie "*".
     * @param from Tabela lub wyrażenie tabelowe (z JOIN), np. "users u JOIN profiles p ON u.id = p.user_id".
     * @return Obiekt QueryBuilder do dalszej konfiguracji i wykonania zapytania.
     */
    fun select(columns: String = "*", from: String): QueryBuilder

    /**
     * Pobiera liczbę wierszy spełniających podane kryteria.
     * Ta metoda pozostaje, ponieważ jej cel jest bardzo specyficzny.
     */
    fun fetchCount(table: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Long

    /**
     * Pobiera pojedynczy wiersz z pełnymi informacjami o pochodzeniu kolumn (nazwa tabeli i kolumny).
     * Używane w specyficznych przypadkach, np. do dynamicznych formularzy.
     */
    fun fetchRowWithColumnInfo(tables: String, filter: String, params: Map<String, Any?> = emptyMap()): Map<ColumnInfo, Any?>?
}

/**
 * Interfejs dla obiektu budującego zapytania (Query Builder).
 * Definiuje wszystkie możliwe operacje konfiguracji zapytania SELECT.
 * Jest częścią publicznego kontraktu `DataFetcher`.
 */
interface QueryBuilder {
    fun where(condition: String?): QueryBuilder
    fun orderBy(ordering: String?): QueryBuilder
    fun limit(count: Int?): QueryBuilder
    fun offset(position: Int): QueryBuilder

    // --- Metody Terminalne (wykonujące zapytanie) ---

    /** Wykonuje zapytanie i zwraca listę map [String, Any?]. To jest domyślny sposób pobierania wielu wierszy. */
    fun toList(params: Map<String, Any?> = emptyMap()): List<Map<String, Any?>>

    /** Wykonuje zapytanie, ustawiając automatycznie LIMIT 1, i zwraca pojedynczą mapę lub null. */
    fun toSingle(params: Map<String, Any?> = emptyMap()): Map<String, Any?>?

    /** Wykonuje zapytanie, ustawiając LIMIT 1, i zwraca wartość z pierwszej kolumny pierwszego wiersza. */
    fun <T> toField(params: Map<String, Any?> = emptyMap()): T?

    /** Wykonuje zapytanie i zwraca listę wartości z pierwszej kolumny. */
    fun <T> toColumn(params: Map<String, Any?> = emptyMap()): List<T>

    /**
     * Wykonuje zapytanie i mapuje wyniki na listę obiektów podanego typu (data class).
     * Wersja dla interfejsu, która przyjmuje KClass jako argument.
     */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): List<T>

    fun <T: Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): T?

}

/**
 * Wygodna funkcja rozszerzająca, która pozwala na użycie składni `toListOf<User>()`.
 * Jest to czysty "syntactic sugar", który ułatwia pracę z API.
 */
inline fun <reified T : Any> QueryBuilder.toListOf(params: Map<String, Any?> = emptyMap()): List<T> {
    return this.toListOf(T::class, params)
}

inline fun <reified T : Any> QueryBuilder.toSingleOf(params: Map<String, Any?> = emptyMap()): T? {
    return this.toSingleOf(T::class, params)
}