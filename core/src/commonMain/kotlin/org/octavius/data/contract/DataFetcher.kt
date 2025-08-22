package org.octavius.data.contract


import kotlin.reflect.KClass

/**
 * Kontrakt dla operacji pobierania danych (SELECT).
 *
 * Uniezależnia logikę biznesową od konkretnej implementacji dostępu do danych.
 */
interface DataFetcher {
    /**
     * Rozpoczyna proces budowania zapytania, zwracając pusty QueryBuilder.
     * Jest to najbardziej elastyczny punkt wejścia, pozwalający na definiowanie
     * klauzul WITH przed głównym SELECT.
     *
     * @return Pusty obiekt QueryBuilder gotowy do konfiguracji.
     */
    fun query(): QueryBuilder

    /**
     * Wygodny skrót do rozpoczynania budowy prostego zapytania SELECT.
     * Równoważne z wywołaniem `query().select(columns, from)`.
     *
     * @param columns Lista kolumn do pobrania. Domyślnie "*".
     * @param from Tabela lub wyrażenie tabelowe.
     * @return Obiekt QueryBuilder z już zdefiniowaną klauzulą SELECT.
     */
    fun select(columns: String = "*", from: String): QueryBuilder

    /**
     * Pobiera liczbę wierszy spełniających podane kryteria.
     */
    fun fetchCount(from: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): DataResult<Long>

    /**
     * Pobiera pojedynczy wiersz z pełnymi informacjami o pochodzeniu kolumn (nazwa tabeli i kolumny).
     * Używane w specyficznych przypadkach, np. do dynamicznych formularzy.
     */
    fun fetchRowWithColumnInfo(tables: String, filter: String, params: Map<String, Any?> = emptyMap()): DataResult<Map<ColumnInfo, Any?>?>
}

/**
 * Interfejs dla obiektu budującego zapytania (Query Builder).
 * Definiuje wszystkie możliwe operacje konfiguracji zapytania SELECT.
 * Jest częścią publicznego kontraktu `DataFetcher`.
 */
interface QueryBuilder {
    /**
     * Dodaje Wspólne Wyrażenie Tabelaryczne (Common Table Expression - CTE) do zapytania.
     * Jest to główny sposób na rozpoczęcie budowy złożonego zapytania.
     * Można wywoływać wielokrotnie w celu dodania kolejnych CTE.
     *
     * @param name Nazwa CTE (np. "regional_sales").
     * @param query Zapytanie SELECT definiujące CTE. Traktowane jako literał.
     * @return Ten sam obiekt QueryBuilder do dalszej konfiguracji.
     */
    fun with(name: String, query: String): QueryBuilder

    /**
     * Oznacza klauzulę WITH jako rekurencyjną.
     *
     * @param recursive Flaga, czy klauzula ma być rekurencyjna. Domyślnie true.
     * @return Ten sam obiekt QueryBuilder do dalszej konfiguracji.
     */
    fun recursive(recursive: Boolean = true): QueryBuilder

    /**
     * Definiuje główną część zapytania SELECT.
     * Ta metoda MUSI być wywołana, aby zapytanie było kompletne.
     *
     * @param columns Lista kolumn do pobrania, np. "id, name, email". Domyślnie "*".
     * @param from Tabela lub wyrażenie tabelowe (z JOIN) bądź podzapytanie, np. "users u JOIN profiles p ON u.id = p.user_id".
     * @return Ten sam obiekt QueryBuilder do dalszej konfiguracji.
     */
    fun select(columns: String = "*", from: String): QueryBuilder

    fun where(condition: String?): QueryBuilder
    fun orderBy(ordering: String?): QueryBuilder
    fun limit(count: Long?): QueryBuilder
    fun offset(position: Long): QueryBuilder

    /**
     * Ustawia paginację w zapytaniu, automatycznie obliczając offset.
     * Jest to standardowy i wygodny sposób na stronicowanie wyników.
     *
     * @param page Numer strony (zaczynając od 0).
     * @param size Liczba wyników na stronie (page size).
     * @return Ten sam obiekt QueryBuilder do dalszej konfiguracji.
     */
    fun page(page: Long, size: Long): QueryBuilder

    // --- Metody Terminalne (wykonujące zapytanie) ---

    /** Wykonuje zapytanie i zwraca listę map [String, Any?]. To jest domyślny sposób pobierania wielu wierszy. */
    fun toList(params: Map<String, Any?> = emptyMap()): DataResult<List<Map<String, Any?>>>

    /** Wykonuje zapytanie, ustawiając automatycznie LIMIT 1, i zwraca pojedynczą mapę lub null. */
    fun toSingle(params: Map<String, Any?> = emptyMap()): DataResult<Map<String, Any?>?>

    /** Wykonuje zapytanie, ustawiając LIMIT 1, i zwraca wartość z pierwszej kolumny pierwszego wiersza. */
    fun <T> toField(params: Map<String, Any?> = emptyMap()): DataResult<T?>

    /** Wykonuje zapytanie i zwraca listę wartości z pierwszej kolumny. */
    fun <T> toColumn(params: Map<String, Any?> = emptyMap()): DataResult<List<T>>

    /**
     * Wykonuje zapytanie i mapuje wyniki na listę obiektów podanego typu (data class).
     * Wersja dla interfejsu, która przyjmuje KClass jako argument.
     */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<List<T>>

    fun <T: Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<T?>

}

inline fun <reified T : Any> QueryBuilder.toListOf(params: Map<String, Any?> = emptyMap()): DataResult<List<T>> {
    return this.toListOf(T::class, params)
}

inline fun <reified T : Any> QueryBuilder.toSingleOf(params: Map<String, Any?> = emptyMap()): DataResult<T?> {
    return this.toSingleOf(T::class, params)
}

