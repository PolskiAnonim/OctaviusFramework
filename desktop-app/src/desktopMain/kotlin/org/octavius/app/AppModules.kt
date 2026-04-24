package org.octavius.app

import org.koin.dsl.module
import org.koin.dsl.onClose
import io.github.octaviusframework.db.core.OctaviusDatabase
import io.github.octaviusframework.db.core.config.DatabaseConfig
import io.github.octaviusframework.db.flyway.FlywayMigrationRunner

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
                dbSchemas = listOf("public", "asian_media", "games", "books", "activity_tracker"),
                setSearchPath = true,
                packagesToScan = listOf("org.octavius"),
            ),
            migrationRunner = FlywayMigrationRunner.create(
                listOf(
                    "public",
                    "asian_media",
                    "games",
                    "books",
                    "activity_tracker"
                ), baselineVersion = "2025.12.21.15.13"
            )
        )
    } onClose { it?.close() }
}