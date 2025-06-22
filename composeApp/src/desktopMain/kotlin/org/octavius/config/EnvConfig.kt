package org.octavius.config

import java.io.File
import java.util.Properties

object EnvConfig {
    private val properties = Properties()
    
    init {
        loadEnvFile()
    }
    
    private fun loadEnvFile() {
        val envFile = File(".env")
        if (envFile.exists()) {
            try {
                envFile.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        val trimmedLine = line.trim()
                        if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                            val parts = trimmedLine.split("=", limit = 2)
                            if (parts.size == 2) {
                                properties[parts[0].trim()] = parts[1].trim()
                            }
                        }
                    }
                }
                println("Załadowano konfigurację z pliku .env")
            } catch (e: Exception) {
                println("Błąd podczas ładowania pliku .env: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("Plik .env nie istnieje, używam wartości domyślnych")
        }
    }
    
    // Funkcja pomocnicza do pobierania wartości z fallback
    private fun getProperty(key: String, defaultValue: String): String {
        return properties.getProperty(key) ?: System.getenv(key) ?: defaultValue
    }
    
    // Konfiguracja bazy danych
    val dbUrl: String get() = getProperty("DB_URL", "jdbc:postgresql://localhost:5430/novels_games")
    val dbUsername: String get() = getProperty("DB_USERNAME", "postgres")
    val dbPassword: String get() = getProperty("DB_PASSWORD", "1234")
    val dbMaxPoolSize: Int get() = getProperty("DB_MAX_POOL_SIZE", "10").toIntOrNull() ?: 10
    
    // Konfiguracja TypeRegistry
    val baseDomainPackage: String get() = getProperty("BASE_DOMAIN_PACKAGE", "org.octavius.domain")
    
    // Schematy bazy danych
    val dbSchemas: List<String> get() = getProperty("DB_SCHEMAS", "public,asian_media,games")
        .split(",").map { it.trim() }
    
    // Funkcja do wyświetlenia aktualnej konfiguracji
    fun printConfig() {
        println("=== Konfiguracja aplikacji ===")
        println("DB URL: $dbUrl")
        println("DB Username: $dbUsername")
        println("DB Max Pool Size: $dbMaxPoolSize")
        println("Base Domain Package: $baseDomainPackage")
        println("DB Schemas: ${dbSchemas.joinToString(", ")}")
        println("===============================")
    }
}