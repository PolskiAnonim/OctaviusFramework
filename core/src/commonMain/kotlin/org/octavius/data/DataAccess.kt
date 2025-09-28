package org.octavius.data

import org.octavius.data.builder.*
import org.octavius.data.transaction.TransactionPlanResults
import org.octavius.data.transaction.TransactionStep

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

    // --- Deklaratywny Batch ---
    fun executeTransactionPlan(steps: List<TransactionStep<*>>): DataResult<TransactionPlanResults>

    // --- Blok Transakcyjny ---
    fun <T> transaction(block: (tx: TransactionalDataAccess) -> DataResult<T>): DataResult<T>
}

/**
 * Definiuje API dostępu do danych w kontekście już istniejącej transakcji.
 * Udostępnia te same buildery co główny interfejs DataAccess, ale nie pozwala
 * na rozpoczynanie nowych transakcji.
 */
interface TransactionalDataAccess {
    fun select(columns: String): SelectQueryBuilder
    fun update(table: String): UpdateQueryBuilder
    fun insertInto(table: String, columns: List<String> = emptyList()): InsertQueryBuilder
    fun deleteFrom(table: String): DeleteQueryBuilder
    fun rawQuery(sql: String): RawQueryBuilder
}