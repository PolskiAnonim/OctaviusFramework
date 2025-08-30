package org.octavius.app

import org.koin.dsl.module
import org.octavius.database.DatabaseSystem

/**
 * Moduł Koin konfigurujący zależności związane z bazą danych.
 *
 * Rejestruje DatabaseSystem jako singleton i udostępnia jego komponenty
 * (DataFetcher i BatchExecutor) jako oddzielne zależności do wstrzykiwania.
 *
 * Struktura:
 * 1. DatabaseSystem - singleton zarządzający połączeniem z bazą danych
 * 2. DataFetcher - komponent do wykonywania zapytań SELECT
 * 3. BatchExecutor - komponent do wykonywania operacji wsadowych (INSERT/UPDATE/DELETE)
 */
val databaseModule = module {
    // 1. Zarejestruj DatabaseSystem jako singleton.
    single { DatabaseSystem() }

    // 2. Udostępnij jego komponenty przez interfejsy.
    // get() automatycznie pobierze instancję DatabaseSystem z definicji powyżej.
    single { get<DatabaseSystem>().fetcher }
    single { get<DatabaseSystem>().batchExecutor }
}