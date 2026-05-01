package org.octavius.settings.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val language: String = "en",
    val database: DatabaseSettings = DatabaseSettings()
)

@Serializable
data class DatabaseSettings(
    val url: String = "jdbc:postgresql://localhost:5432/octavius",
    val username: String = "postgres",
    val password: String = "1234"
)
