package org.octavius.util

/**
 * implementacja `loadResources` dla platformy JVM.
 * Używa ClassLoader do przeszukania całego classpath i znalezienia
 * wszystkich pasujących plików zasobów z różnych modułów (JAR-ów).
 */
actual fun loadResources(name: String): List<String> {
    val resources = mutableListOf<String>()
    try {
        val classLoader = Thread.currentThread().contextClassLoader
        val resourceUrls = classLoader.getResources(name)

        resourceUrls.asSequence().forEach { url ->
            try {
                resources.add(url.readText())
            } catch (e: Exception) {
                println("WARNING: Could not read resource: $url. Reason: ${e.message}")
            }
        }
    } catch (e: Exception) {
        println("ERROR: Could not scan for resources '$name'. Reason: ${e.message}")
        e.printStackTrace()
    }
    return resources
}