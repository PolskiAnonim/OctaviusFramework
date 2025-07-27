package org.octavius.database

internal object DatabaseConfig {
    // Konfiguracja bazy danych
    val dbUrl: String get() = "jdbc:postgresql://localhost:5430/novels_games"
    val dbUsername: String get() = "postgres"
    val dbPassword: String get() = "1234"

    val dbMaxPoolSize: Int get() = 10

    // Konfiguracja TypeRegistry
    val baseDomainPackage: String get() = "org.octavius.domain"

    // Schematy bazy danych
    val dbSchemas: List<String> get() = listOf("public", "asian_media", "games")
}