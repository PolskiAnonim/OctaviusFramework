package org.octavius.data.builder

/**
 * Definiuje publiczne API do budowania zapytań SQL SELECT.
 */
interface SelectQueryBuilder: TerminalReturningMethods, StepConvertible {

    /** Dodaje do zapytania Wspólne Wyrażenie Tabelaryczne (CTE). */
    fun with(name: String, query: String): SelectQueryBuilder

    /** Oznacza klauzulę WITH jako rekurencyjną. */
    fun recursive(recursive: Boolean = true): SelectQueryBuilder

    /** Definiuje źródło danych (klauzula FROM). */
    fun from(source: String): SelectQueryBuilder

    /**
     * Używa wyniku innego zapytania jako źródła danych (tabela pochodna).
     * Automatycznie opakowuje podzapytanie w nawiasy i dodaje alias gdy jest podany.
     *
     * @param subquery String SQL zawierający podzapytanie.
     * @param alias Nazwa (alias), która zostanie nadana tabeli pochodnej.
     */
    fun fromSubquery(subquery: String, alias: String? = null): SelectQueryBuilder

    /** Definiuje warunek filtrowania (klauzula WHERE). */
    fun where(condition: String?): SelectQueryBuilder

    /** Definiuje grupowanie wierszy (klauzula GROUP BY). */
    fun groupBy(columns: String?): SelectQueryBuilder

    /** Filtruje wyniki po grupowaniu (klauzula HAVING). */
    fun having(condition: String?): SelectQueryBuilder

    /** Definiuje sortowanie wyników (klauzula ORDER BY). */
    fun orderBy(ordering: String?): SelectQueryBuilder

    /** Ogranicza liczbę zwracanych wierszy (klauzula LIMIT). */
    fun limit(count: Long?): SelectQueryBuilder

    /** Określa liczbę wierszy do pominięcia (klauzula OFFSET). */
    fun offset(position: Long): SelectQueryBuilder

    /** Konfiguruje paginację, ustawiając LIMIT i OFFSET.
     * @param page Numer strony (indeksowany od zera).
     * @param size Rozmiar strony
     */
    fun page(page: Long, size: Long): SelectQueryBuilder
}

/**
 * Definiuje publiczne API do budowania zapytań SQL DELETE.
 */
interface DeleteQueryBuilder : TerminalReturningMethods, TerminalModificationMethods, StepConvertible {

    /** Dodaje do zapytania Wspólne Wyrażenie Tabelaryczne (CTE). */
    fun with(name: String, query: String): DeleteQueryBuilder

    /** Oznacza klauzulę WITH jako rekurencyjną.*/
    fun recursive(recursive: Boolean = true): DeleteQueryBuilder

    /** Dodaje klauzulę USING */
    fun using(tables: String): DeleteQueryBuilder

    /** Definiuje warunek WHERE. Klauzula jest obowiązkowa ze względów bezpieczeństwa. */
    fun where(condition: String): DeleteQueryBuilder

    /**
     * Dodaje klauzulę RETURNING. Wymaga użycia metod `.toList()`, `.toSingle()` itp.
     * zamiast `.execute()`.
     */
    fun returning(vararg columns: String): DeleteQueryBuilder
}

/**
 * Definiuje publiczne API do budowania zapytań SQL UPDATE.
 */
interface UpdateQueryBuilder : TerminalReturningMethods, TerminalModificationMethods, StepConvertible {

    /** Dodaje do zapytania Wspólne Wyrażenie Tabelaryczne (CTE). */
    fun with(name: String, query: String): UpdateQueryBuilder

    /** Oznacza klauzulę WITH jako rekurencyjną. */
    fun recursive(recursive: Boolean = true): UpdateQueryBuilder

    /**
     * Definiuje przypisania w klauzuli SET.
     */
    fun setExpressions(values: Map<String, String>): UpdateQueryBuilder

    /** Definiuje pojedyncze przypisanie w klauzuli SET. */
    fun setExpression(column: String, value: String): UpdateQueryBuilder

    /** Definiuje pojedyncze przypisanie w klauzuli SET Automatycznie generuje placeholder o nazwie klucza. */
    fun setValue(column: String): UpdateQueryBuilder

    /**
     * Ustawia wartości do aktualizacji. Automatycznie generuje placeholdery
     * w formacie ":key" dla każdego klucza w mapie.
     * Wartości z mapy muszą być przekazane w metodzie terminalnej (np. .execute()).
     */
    fun setValues(values: Map<String, Any?>): UpdateQueryBuilder

    /**
     * Dodaje do zapytania UPDATE klauzulę FROM.
     */
    fun from(tables: String): UpdateQueryBuilder

    /**
     * Definiuje warunek WHERE. Klauzula jest obowiązkowa ze względów bezpieczeństwa.
     */
    fun where(condition: String): UpdateQueryBuilder

    /**
     * Dodaje klauzulę RETURNING. Wymaga użycia metod `.toList()`, `.toSingle()` itp.
     * zamiast `.execute()`.
     */
    fun returning(vararg columns: String): UpdateQueryBuilder
}

/**
 * Definiuje publiczne API do budowania zapytań SQL INSERT.
 */
interface InsertQueryBuilder : TerminalReturningMethods, TerminalModificationMethods, StepConvertible {

    /** Dodaje do zapytania Wspólne Wyrażenie Tabelaryczne (CTE). */
    fun with(name: String, query: String): InsertQueryBuilder

    /** Oznacza klauzulę WITH jako rekurencyjną. */
    fun recursive(recursive: Boolean = true): InsertQueryBuilder

    /**
     * Definiuje wartości do wstawienia jako wyrażenia SQL lub placeholdery.
     * To jest metoda niskopoziomowa.
     * @param expressions Mapa, gdzie klucz to nazwa kolumny, a wartość to string SQL (np. ":name", "NOW()").
     */
    fun valuesExpressions(expressions: Map<String, String>): InsertQueryBuilder

    /**
     * Definiuje pojedynczą wartość do wstawienia jako wyrażenie SQL.
     * @param column Nazwa kolumny.
     * @param expression Wyrażenie SQL (np. ":user_id", "DEFAULT").
     */
    fun valueExpression(column: String, expression: String): InsertQueryBuilder

    /**
     * Definiuje wartości do wstawienia, automatycznie generując placeholdery.
     * To jest preferowana, wysokopoziomowa metoda do wstawiania danych.
     * Wartości z mapy muszą być przekazane w metodzie terminalnej (np. .execute()).
     *
     * @param data Mapa danych (kolumna -> wartość).
     */
    fun values(data: Map<String, Any?>): InsertQueryBuilder

    /**
     * Definiuje pojedynczą wartość, automatycznie generując placeholder.
     * @param column Nazwa kolumny, dla której zostanie wygenerowany placeholder (np. ":nazwa_kolumny").
     */
    fun value(column: String): InsertQueryBuilder

    /**
     * Definiuje zapytanie SELECT jako źródło danych do wstawienia.
     * Wymaga, aby kolumny były zdefiniowane podczas tworzenia buildera (w `insertInto`).
     * Wyklucza użycie wszelkiego rodzaju funkcji `value(s)`.
     */
    fun fromSelect(query: String): InsertQueryBuilder

    /**
     * Konfiguruje zachowanie w przypadku konfliktu klucza (klauzula ON CONFLICT).
     */
    fun onConflict(config: OnConflictClauseBuilder.() -> Unit): InsertQueryBuilder

    /**
     * Dodaje klauzulę RETURNING. Wymaga użycia `.toList()`, `.toSingle()` itp. zamiast `.execute()`.
     */
    fun returning(vararg columns: String): InsertQueryBuilder
}

/**
 * Konfigurator dla klauzuli ON CONFLICT w zapytaniu INSERT.
 */
interface OnConflictClauseBuilder {

    /** Definiuje cel konfliktu jako listę kolumn. */
    fun onColumns(vararg columns: String)

    /** Definiuje cel konfliktu jako nazwę istniejącego ograniczenia (constraint). */
    fun onConstraint(constraintName: String)

    /** W przypadku konfliktu, nie rób nic (DO NOTHING). */
    fun doNothing()

    /**
     * W przypadku konfliktu, wykonaj aktualizację (DO UPDATE).
     * @param setExpression Wyrażenie SET, np. "counter = tbl.counter + 1". Użyj `EXCLUDED` do odwołania się do wartości, które próbowano wstawić.
     * @param whereCondition Opcjonalny warunek WHERE dla akcji UPDATE.
     */
    fun doUpdate(setExpression: String, whereCondition: String? = null)
}

/**
 * Definiuje publiczne API do przekazania pełnego zapytania.
 */
interface RawQueryBuilder : TerminalReturningMethods, TerminalModificationMethods, StepConvertible {
    // Tylko metody terminalne które są brane z innych interfejsów
}