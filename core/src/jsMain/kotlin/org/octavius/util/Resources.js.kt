package org.octavius.util


actual fun loadResources(name: String): List<String> {
    // TODO: Zaimplementować ładowanie zasobów w środowisku JS, jeśli będzie potrzebne.
    println("WARNING: Resource loading not yet implemented for JS target. Resource '$name' will be empty.")
    return emptyList()
}