package org.octavius.app

import org.koin.dsl.module
import org.octavius.database.DatabaseSystem

val databaseModule = module {
    // 1. Zarejestruj DatabaseSystem jako singleton.
    single { DatabaseSystem() }

    // 2. Udostępnij jego komponenty przez interfejsy.
    // get() automatycznie pobierze instancję DatabaseSystem z definicji powyżej.
    single { get<DatabaseSystem>().fetcher }
    single { get<DatabaseSystem>().transactionManager }
}