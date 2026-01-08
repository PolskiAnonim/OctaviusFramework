package org.octavius.util

/**
 * implementacja `loadResources` dla platformy JVM.
 * Używa ClassLoader do przeszukania całego classpath i znalezienia
 * wszystkich pasujących plików zasobów z różnych modułów (JAR-ów).
 */
/**
 * Implementacja `loadResource` dla platformy JVM.
 * Po wprowadzeniu taska Gradle'a, ta funkcja ładuje jeden,
 * wcześniej połączony plik zasobów, który znajduje się w classpath.
 */
actual fun loadResource(name: String): String? {
    try {
        val classLoader = Thread.currentThread().contextClassLoader

        val resourceUrl = classLoader.getResource(name)

        if (resourceUrl != null) {
            return try {
                resourceUrl.readText()
            } catch (e: Exception) {
                println("WARNING: Could not read resource: $resourceUrl. Reason: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        println("ERROR: Could not load resource '$name'. Reason: ${e.message}")
        e.printStackTrace()
    }
    return null
}