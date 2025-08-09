package org.octavius.util

/**
 *
 * @param name Nazwa pliku zasobu do znalezienia (np. "translations_pl.json").
 * @return Lista zawartości znalezionych plików jako String.
 */
expect fun loadResources(name: String): List<String>