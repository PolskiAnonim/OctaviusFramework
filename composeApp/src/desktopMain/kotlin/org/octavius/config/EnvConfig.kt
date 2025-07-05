package org.octavius.config

import java.io.File
import java.util.*

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
    private fun getProperty(key: String): String {
        return properties.getProperty(key) ?: System.getenv(key)
    }
    
    // Konfiguracja bazy danych
    val dbUrl: String get() = getProperty("DB_URL")
    val dbUsername: String get() = getProperty("DB_USERNAME")
    val dbPassword: String get() = getProperty("DB_PASSWORD")
    val dbMaxPoolSize: Int get() = getProperty("DB_MAX_POOL_SIZE").toIntOrNull() ?: 10
    
    // Konfiguracja TypeRegistry
    val baseDomainPackage: String get() = getProperty("BASE_DOMAIN_PACKAGE")
    
    // Schematy bazy danych
    val dbSchemas: List<String> get() = getProperty("DB_SCHEMAS")
        .split(",").map { it.trim() }

    val language: String get() = getProperty("LANGUAGE")

    // Funkcja do wyświetlenia aktualnej konfiguracji
    fun printConfig() {
        println("=== Konfiguracja aplikacji ===")
        println("DB URL: $dbUrl")
        println("DB Username: $dbUsername")
        println("DB Max Pool Size: $dbMaxPoolSize")
        println("Base Domain Package: $baseDomainPackage")
        println("DB Schemas: ${dbSchemas.joinToString(", ")}")
        println("Language : $language")
        println("===============================")
    }
}