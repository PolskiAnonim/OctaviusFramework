package org.octavius.app

import org.koin.dsl.module
import org.koin.dsl.onClose
import io.github.octaviusframework.db.core.OctaviusDatabase
import io.github.octaviusframework.db.core.config.DatabaseConfig
import io.github.octaviusframework.db.flyway.FlywayMigrationRunner
import org.octavius.app.settings.AppSettingsManager

/**
 * Moduł Koin konfigurujący zależności związane z bazą danych.
 */
val databaseModule = module {
    single {
        val settings = get<AppSettingsManager>().currentSettings.database
        OctaviusDatabase.fromConfig(
            DatabaseConfig(
                dbUrl = settings.url,
                dbUsername = settings.username,
                dbPassword = settings.password,
                dbSchemas = listOf("public", "asian_media", "games", "books", "finances"),
                setSearchPath = true,
                packagesToScan = listOf("org.octavius"),
            ),
            migrationRunner = FlywayMigrationRunner.create(
                listOf(
                    "public",
                    "asian_media",
                    "games",
                    "books",
                    "finances"
                ), baselineVersion = "2025.12.21.15.13"
            )
        )
    } onClose { it?.close() }
}