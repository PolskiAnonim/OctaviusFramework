package org.octavius.app

import org.koin.dsl.module
import org.octavius.database.DatabaseConfig
import org.octavius.database.OctaviusDatabase

/**
 * Moduł Koin konfigurujący zależności związane z bazą danych.
 */
val databaseModule = module {
    single {
        OctaviusDatabase.fromConfig(
            DatabaseConfig(
                dbUrl = "jdbc:postgresql://localhost:5432/octavius",
                dbUsername = "postgres",
                dbPassword = "1234",
                dbSchemas = listOf("public", "asian_media", "games"),
                setSearchPath = true,
                packagesToScan = listOf("org.octavius")
            )
        )
    }
}