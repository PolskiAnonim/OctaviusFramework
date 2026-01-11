package org.octavius.app

import org.koin.dsl.module
import org.octavius.database.OctaviusDatabase
import org.octavius.database.config.DatabaseConfig

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
                dbSchemas = listOf("public", "asian_media", "games", "books"),
                setSearchPath = true,
                packagesToScan = listOf("org.octavius"),
                flywayBaselineVersion = "2025.12.21.15.13"
            )
        )
    }
}