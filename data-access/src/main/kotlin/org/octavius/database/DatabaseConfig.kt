package org.octavius.database

import java.util.*

internal object DatabaseConfig {
    // Domyślne wartości
    var dbUrl = "jdbc:postgresql://localhost:5430/novels_games"
    var dbUsername = "postgres"
    var dbPassword = "1234"
    var dbSchemas: List<String>  = listOf("public", "asian_media", "games")
    var baseDomainPackage = "org.octavius.domain"

    // Funkcja do ładowania konfiguracji z pliku
    fun loadFromFile(fileName: String) {
        try {
            val props = Properties()
            val resourceStream = this::class.java.classLoader.getResourceAsStream(fileName)
            if (resourceStream != null) {
                props.load(resourceStream)
                dbUrl = props.getProperty("db.url")
                dbUsername = props.getProperty("db.username")
                dbPassword = props.getProperty("db.password")
                dbSchemas = props.getProperty("db.schemas").split(",")
                baseDomainPackage = props.getProperty("db.baseDomainPackage")
            } else {
                println("WARNING: Could not find properties file '$fileName'. Using default config.")
            }
        } catch (e: Exception) {
            println("ERROR: Failed to load database config from '$fileName'.")
            e.printStackTrace()
        }
    }
}