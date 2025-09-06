package org.octavius.data.contract

import kotlin.reflect.KClass

/**
 * Kontrakt dla operacji pobierania danych (SELECT).
 *
 * Interfejs ten uniezależnia logikę biznesową od konkretnej implementacji dostępu do danych.
 * Implementacje tego interfejsu powinny zapewniać, że wszelki generowany wewnętrznie SQL używa parametrów,
 * które są ostatecznie przekazywane poprzez metody terminalne [QueryBuilder].
 */
interface DataFetcher {
    /**
     * Rozpoczyna proces budowania zapytania, zwracając pusty [QueryBuilder].
     * Jest to najbardziej elastyczny punkt wejścia, pozwalający na definiowanie klauzul `WITH`
     * przed głównym zapytaniem `SELECT`.
     *
     * @return Pusty obiekt [QueryBuilder] gotowy do konfiguracji.
     */
    fun query(): QueryBuilder

    /**
     * Wygodny skrót do rozpoczynania budowy prostego zapytania `SELECT`.
     * Równoważne z wywołaniem `query().select(columns).from(from)`.
     *
     * @param columns Lista kolumn do pobrania, np. "id, name". Domyślnie "*".
     * @param from Tabela lub wyrażenie tabelowe (np. "users u JOIN profiles p ON u.id = p.user_id").
     * @return Obiekt [QueryBuilder] z już zdefiniowaną klauzulą `SELECT` i `FROM`.
     */
    fun select(columns: String = "*", from: String): QueryBuilder
}

/**
 * Interfejs do programistycznego budowania zapytań SQL.
 * Definiuje wszystkie dostępne operacje konfiguracji zapytania `SELECT`
 * i jest kluczowym elementem publicznego kontraktu [DataFetcher].
 *
 * Wszystkie metody konfiguracyjne (z wyjątkiem [with]) nadpisują poprzednio ustawione wartości
 * dla danej klauzuli.
 */
interface QueryBuilder {
    /**
     * Dodaje Wspólne Wyrażenie Tabelaryczne (Common Table Expression - CTE) do zapytania.
     * Jest to główny sposób na konstruowanie złożonych zapytań wymagających pośrednich wyników.
     * Można wywoływać wielokrotnie w celu dodania kolejnych CTE; każdy wywołanie dodaje nowe CTE.
     *
     * @param name Nazwa CTE (np. "regional_sales").
     * @param query Zapytanie `SELECT` definiujące CTE. Ten ciąg jest traktowany jako literał
     *              i sam powinien być sparametryzowany, jeśli w definicji CTE używane są dane wejściowe użytkownika.
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun with(name: String, query: String): QueryBuilder

    /**
     * Oznacza klauzulę `WITH` jako rekurencyjną.
     * Jest to typowo używane do przechodzenia przez struktury hierarchiczne lub grafowe.
     *
     * @param recursive Ustaw na `true`, aby blok `WITH` był rekurencyjny; `false` w przeciwnym razie. Domyślnie `true`.
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun recursive(recursive: Boolean = true): QueryBuilder

    /**
     * Definiuje główną klauzulę `SELECT` zapytania.
     * Wielokrotne wywołania nadpisują poprzednie ustawienia.
     *
     * @param columns Lista kolumn do pobrania, np. "id, name, email". Domyślnie "*".
     *
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun select(columns: String = "*"): QueryBuilder

    /**
     * Definiuje klauzulę `FROM` zapytania.
     * Musi być wywołana przed metodą terminalną. Wielokrotne wywołania nadpisują poprzednie ustawienia.
     *
     * @param query Tabela, wyrażenie tabelowe (z `JOIN`ami) lub podzapytanie, z którego mają być pobrane dane,
     *             np. "users u JOIN profiles p ON u.id = p.user_id".
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun from(query: String): QueryBuilder

    /**
     * Dodaje klauzulę `WHERE` do zapytania.
     * Wielokrotne wywołania nadpisują poprzednie ustawienia.
     *
     * @param condition Warunek klauzuli `WHERE`, np. "id = :id AND status = 'active'". Użyj nazwanych parametrów dla wartości.
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun where(condition: String?): QueryBuilder

    /**
     * Dodaje klauzulę GROUP BY do zapytania.
     * @param columns Lista kolumn do grupowania, oddzielonych przecinkami.
     */
    fun groupBy(columns: String?): QueryBuilder

    /**
     * Dodaje klauzulę HAVING do zapytania, która filtruje wyniki po grupowaniu.
     * Wymaga uprzedniego zdefiniowania klauzuli GROUP BY.
     * @param condition Warunek do filtrowania grup.
     */
    fun having(condition: String?): QueryBuilder

    /**
     * Dodaje klauzulę `ORDER BY` do zapytania.
     * Wielokrotne wywołania nadpisują poprzednie ustawienia.
     *
     * @param ordering Kryteria sortowania, np. "name ASC, id DESC".
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun orderBy(ordering: String?): QueryBuilder

    /**
     * Dodaje klauzulę `LIMIT` do zapytania, ograniczając maksymalną liczbę zwracanych wierszy.
     * Wielokrotne wywołania nadpisują poprzednie ustawienia.
     *
     * @param count Maksymalna liczba wierszy do zwrócenia. Wartość `null` lub `0` zazwyczaj oznacza brak limitu.
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun limit(count: Long?): QueryBuilder

    /**
     * Dodaje klauzulę `OFFSET` do zapytania, określającą liczbę wierszy do pominięcia od początku zbioru wyników.
     * Wielokrotne wywołania nadpisują poprzednie ustawienia.
     *
     * @param position Liczba wierszy do pominięcia od początku zbioru wyników.
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun offset(position: Long): QueryBuilder

    /**
     * Konfiguruje paginację dla zapytania, automatycznie obliczając `OFFSET` i `LIMIT` na podstawie podanego numeru strony i rozmiaru.
     * Jest to wygodna metoda do stronicowania zbioru wyników. Nadpisuje wszelkie wcześniejsze wywołania [limit] i [offset].
     *
     * @param page Numer strony (indeksowany od 0).
     * @param size Liczba wyników na stronie (rozmiar strony).
     * @return Ta instancja [QueryBuilder] do dalszego łańcuchowania metod.
     */
    fun page(page: Long, size: Long): QueryBuilder

    // --- Metody Terminalne (wykonujące zapytanie) ---

    /**
     * Wykonuje skonstruowane zapytanie i zwraca [DataResult] zawierający listę wierszy.
     * Każdy wiersz jest reprezentowany jako `Map<String, Any?>`, gdzie kluczami są aliasy/nazwy kolumn, a wartościami odpowiadające im dane.
     * Jest to domyślny i najbardziej elastyczny sposób pobierania wielu wierszy, gdy nie jest używana konkretna klasa danych.
     *
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem (np. warunki klauzuli `WHERE`).
     * @return [DataResult] zawierający listę wierszy jako `List<Map<String, Any?>>` lub błąd.
     */
    fun toList(params: Map<String, Any?> = emptyMap()): DataResult<List<Map<String, Any?>>>

    /**
     * Wykonuje skonstruowane zapytanie, niejawnie stosując `LIMIT 1`, i zwraca [DataResult]
     * zawierający pojedynczy wiersz jako `Map<String, Any?>` lub `null`, jeśli nie znaleziono pasującego wiersza.
     *
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający pojedynczy wiersz jako `Map<String, Any?>` lub `null`, jeśli nie znaleziono wiersza, albo błąd.
     */
    fun toSingle(params: Map<String, Any?> = emptyMap()): DataResult<Map<String, Any?>?>

    /**
     * Wykonuje skonstruowane zapytanie, niejawnie stosując `LIMIT 1`, i zwraca [DataResult]
     * zawierający wartość z pierwszej kolumny pierwszego wiersza. Użyteczne dla funkcji agregujących
     * (np. `COUNT(*)`, `SUM(kolumna)`). Jeśli klauzula `SELECT` nie została jawnie zdefiniowana,
     * zostanie użyte `SELECT *` jako domyślne.
     *
     * @param T Oczekiwany typ pola.
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający wartość pierwszej kolumny z pierwszego wiersza,
     *         lub `null`, jeśli nie znaleziono wiersza/wartości, albo błąd.
     */
    fun <T> toField(params: Map<String, Any?> = emptyMap()): DataResult<T?>

    /**
     * Wykonuje skonstruowane zapytanie i zwraca [DataResult] zawierający listę wartości
     * z pierwszej kolumny każdego wiersza. Jest to użyteczne do pobierania pojedynczej listy identyfikatorów, nazw itp.
     * Jeśli klauzula `SELECT` nie została jawnie zdefiniowana, zostanie użyte `SELECT *` jako domyślne.
     *
     * @param T Oczekiwany typ wartości kolumn.
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający listę wartości z pierwszej kolumny lub błąd.
     */
    fun <T> toColumn(params: Map<String, Any?> = emptyMap()): DataResult<List<T?>>

    /**
     * Wykonuje skonstruowane zapytanie i mapuje wyniki na listę obiektów podanego typu
     * (zazwyczaj klasa danych). Ta wersja jawnie przyjmuje argument [KClass].
     * Jeśli klauzula `SELECT` nie została jawnie zdefiniowana, zostanie użyte `SELECT *` jako domyślne.
     *
     * @param T Typ obiektów, na które mają być mapowane wyniki.
     * @param kClass Obiekt [KClass] reprezentujący docelową klasę danych. Nazwy kolumn z zapytania
     *               powinny idealnie pasować do nazw właściwości tej klasy w zmienionej konwencji
     *               nazewniczej (snake_case) w celu automatycznego mapowania.
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający listę obiektów `T` lub błąd.
     */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<List<T>>
    /**
     * Wykonuje skonstruowane zapytanie, niejawnie stosując `LIMIT 1`, i mapuje wynik na pojedynczy
     * obiekt podanego typu (zazwyczaj klasa danych) lub `null`, jeśli nie znaleziono pasującego wiersza.
     * Jeśli klauzula `SELECT` nie została jawnie zdefiniowana, zostanie użyte `SELECT *` jako domyślne.
     *
     * @param T Typ obiektu, na który ma być mapowany wynik.
     * @param kClass Obiekt [KClass] reprezentujący docelową klasę danych. Nazwy kolumn z zapytania
     *               powinny idealnie pasować do nazw właściwości tej klasy w zmienionej konwencji
     *               nazewniczej (snake_case) w celu automatycznego mapowania.
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający pojedynczy obiekt `T` lub `null`, jeśli nie znaleziono wiersza, albo błąd.
     */
    fun <T: Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<T?>

    /**
     * Wykonuje skonstruowane zapytanie, niejawnie stosując `LIMIT 1`, i zwraca [DataResult]
     * zawierający mapę, gdzie kluczami są obiekty [ColumnInfo], a wartościami dane kolumn.
     * Jeśli klauzula `SELECT` nie została jawnie zdefiniowana, zostanie użyte `SELECT *` jako domyślne.
     *
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający mapę, gdzie kluczami są obiekty [ColumnInfo], a wartościami dane kolumn.
     *         Zwraca `null`, jeśli nie znaleziono wiersza.
     */
    fun toSingleWithColumnInfo(params: Map<String, Any?> = emptyMap()): DataResult<Map<ColumnInfo, Any?>?>

    /**
     * Wykonuje skonstruowane zapytanie i zwraca [DataResult] zawierający liczbę wierszy jako [Long].
     * Jeśli klauzula `SELECT` nie została jawnie zdefiniowana, zostanie użyte `SELECT COUNT(*)` jako domyślne.
     *
     * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
     * @return [DataResult] zawierający liczbę jako [Long] lub błąd.
     */
    fun toCount(params: Map<String, Any?> = emptyMap()): DataResult<Long>

    /**
     * Buduje i zwraca ostateczny string zapytania SQL na podstawie
     * bieżącej konfiguracji buildera.
     * Nie wykonuje zapytania.
     *
     * @return String zawierający gotowe do wykonania zapytanie SQL.
     */
    fun toSql(defaultSelect: String = "*"): String
}

/**
 * Wygodna funkcja rozszerzająca `inline` do wykonywania zapytania i mapowania wyników na listę obiektów typu `T`.
 * Wykorzystuje `reified` do automatycznego wnioskowania `T::class`.
 *
 * @param T Typ obiektów, na które mają być mapowane wyniki.
 * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
 * @return [DataResult] zawierający listę obiektów `T` lub błąd.
 */
inline fun <reified T : Any> QueryBuilder.toListOf(params: Map<String, Any?> = emptyMap()): DataResult<List<T>> {
    return this.toListOf(T::class, params)
}

/**
 * Wygodna funkcja rozszerzająca `inline` do wykonywania zapytania, niejawnego stosowania `LIMIT 1`,
 * i mapowania wyniku na pojedynczy obiekt typu `T`, lub `null`, jeśli nie znaleziono pasującego wiersza.
 * Wykorzystuje `reified` do automatycznego wnioskowania `T::class`.
 *
 * @param T Typ obiektu, na który ma być mapowany wynik.
 * @param params Mapa nazwanych parametrów do powiązania z zapytaniem.
 * @return [DataResult] zawierający pojedynczy obiekt `T` lub `null`, jeśli nie znaleziono wiersza, albo błąd.
 */
inline fun <reified T : Any> QueryBuilder.toSingleOf(params: Map<String, Any?> = emptyMap()): DataResult<T?> {
    return this.toSingleOf(T::class, params)
}