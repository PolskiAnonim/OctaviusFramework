package org.octavius.data

import org.octavius.data.builder.*
import org.octavius.data.transaction.TransactionPlanResults
import org.octavius.data.transaction.TransactionStep

/**
 * Definiuje kontrakt dla podstawowych operacji na bazie danych (CRUD i zapytania surowe).
 *
 * Ten interfejs stanowi fundament, który jest wykorzystywany zarówno do wykonywania
 * pojedynczych zapytań, jak i operacji wewnątrz bloku transakcyjnego.
 */
interface QueryOperations {

    /**
     * Rozpoczyna budowanie zapytania SELECT.
     *
     * @param columns Lista kolumn do pobrania. Musi być podana co najmniej jedna.
     * @return Nowa instancja buildera dla zapytania SELECT.
     */
    fun select(vararg columns: String): SelectQueryBuilder

    /**
     * Rozpoczyna budowanie zapytania UPDATE.
     *
     * @param table Nazwa tabeli do aktualizacji.
     * @return Nowa instancja buildera dla zapytania UPDATE.
     */
    fun update(table: String): UpdateQueryBuilder

    /**
     * Rozpoczyna budowanie zapytania INSERT.
     *
     * @param table Nazwa tabeli, do której wstawiane są dane.
     * @param columns Opcjonalna lista kolumn. Jeśli pominięta, wartości muszą być podane
     *                dla wszystkich kolumn w tabeli w odpowiedniej kolejności.
     * @return Nowa instancja buildera dla zapytania INSERT.
     */
    fun insertInto(table: String, columns: List<String> = emptyList()): InsertQueryBuilder

    /**
     * Rozpoczyna budowanie zapytania DELETE.
     *
     * @param table Nazwa tabeli, z której usuwane są dane.
     * @return Nowa instancja buildera dla zapytania DELETE.
     */
    fun deleteFrom(table: String): DeleteQueryBuilder

    /**
     * Umożliwia wykonanie surowego zapytania SQL.
     *
     * @param sql Zapytanie SQL do wykonania, może zawierać nazwane parametry (np. `:userId`).
     * @return Nowa instancja buildera dla surowego zapytania.
     */
    fun rawQuery(sql: String): RawQueryBuilder
}

/**
 * Główny punkt wejścia do warstwy danych, oferujący spójne API do interakcji z bazą danych.
 *
 * Fasada ta umożliwia:
 * 1. Wykonywanie pojedynczych zapytań (CRUD) w trybie auto-commit, dzięki implementacji [QueryOperations].
 * 2. Wykonywanie atomowych, złożonych operacji w ramach zarządzanych bloków transakcyjnych.
 * 3. Uruchamianie predefiniowanych, deklaratywnych planów transakcyjnych.
 */
interface DataAccess : QueryOperations {

    /**
     * Wykonuje sekwencję operacji (plan) w ramach jednej, atomowej transakcji.
     *
     * Idealne rozwiązanie dla scenariuszy, gdzie kroki transakcji są budowane dynamicznie,
     * np. na podstawie danych z formularza.
     *
     * @param steps Lista kroków transakcji do wykonania.
     * @return [DataResult] zawierający [TransactionPlanResults] w przypadku sukcesu lub błąd w razie niepowodzenia.
     *         Transakcja jest automatycznie wycofywana w przypadku błędu.
     */
    fun executeTransactionPlan(steps: List<TransactionStep<*>>): DataResult<TransactionPlanResults>

    /**
     * Wykonuje podany blok kodu w ramach nowej, zarządzanej transakcji.
     *
     * Zapewnia, że wszystkie operacje wewnątrz bloku `block` zostaną wykonane atomowo.
     * Transakcja zostanie zatwierdzona (commit) tylko wtedy, gdy blok zakończy się sukcesem
     * i zwróci [DataResult.Success]. W każdym innym przypadku (zwrócenie [DataResult.Failure]
     * lub rzucenie wyjątku), transakcja zostanie automatycznie wycofana (rollback).
     *
     * @param block Lambda, która otrzymuje kontekst [QueryOperations] do wykonywania operacji bazodanowych.
     *              Ten kontekst jest aktywny tylko w ramach tej transakcji.
     * @return [DataResult] z wynikiem operacji z bloku (`T`) lub błędem.
     */
    fun <T> transaction(block: (tx: QueryOperations) -> DataResult<T>): DataResult<T>
}