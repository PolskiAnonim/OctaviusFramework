package org.octavius.data.contract

import org.octavius.data.contract.builder.*

/**
 * Główny kontrakt dla interakcji z warstwą dostępu do danych.
 *
 * Fasada ta oferuje trzy paradygmaty dostępu do bazy danych:
 * 1. Fluent Builders: do budowania i wykonywania pojedynczych, skomplikowanych zapytań SQL.
 * 2. Deklaratywny Batch: do wykonywania sekwencji operacji (planu) w jednej transakcji,
 *    idealny dla dynamicznych formularzy.
 * 3. Imperatywny Blok Transakcyjny: do hermetyzacji złożonej logiki biznesowej w atomowe operacje.
 */
interface DataAccess {
    // --- Fluent Builders ---

    /**
     * Rozpoczyna budowanie zapytania SELECT.
     * @param columns Lista kolumn do pobrania
     */
    fun select(columns: String): SelectQueryBuilder

    /** Rozpoczyna budowanie zapytania UPDATE dla podanej tabeli. */
    fun update(table: String): UpdateQueryBuilder

    /** Rozpoczyna budowanie zapytania INSERT dla podanej tabeli. */
    fun insertInto(table: String, columns: List<String> = emptyList()): InsertQueryBuilder

    /** Rozpoczyna budowanie zapytania DELETE dla podanej tabeli. */
    fun deleteFrom(table: String): DeleteQueryBuilder

    /** Umożliwia wykonanie dowolnego, surowego zapytania SQL. */
    fun rawQuery(sql: String): RawQueryBuilder

//    // --- Deklaratywny Batch ---
//    fun executeBatch(steps: List<TransactionStep>): DataResult<BatchStepResults>
//
//    // --- Blok Transakcyjny ---
//    fun <T> transaction(block: (tx: TransactionalDataAccess) -> DataResult<T>): DataResult<T>
}

/**
 * Specjalna wersja [DataAccess] przeznaczona do użycia wewnątrz bloku transakcyjnego.
 * Zapewnia, że wszystkie operacje są wykonywane w kontekście tej samej transakcji.
 */
interface TransactionalDataAccess {
    fun select(columns: String = "*", from: String): SelectQueryBuilder
    fun select(): SelectQueryBuilder // do bardziej wykorzystania domyślnych kolumn lub dla braku FROM
    fun update(table: String): UpdateQueryBuilder
    fun insertInto(table: String): InsertQueryBuilder
    fun deleteFrom(table: String): DeleteQueryBuilder

    // Uwaga: Brak `executeBatch` i `transaction`, aby uniknąć zagnieżdżania.
}