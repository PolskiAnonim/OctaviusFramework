package org.octavius.util

/**
 * Ładuje zawartość pliku o podanej nazwie z zasobów aplikacji.
 *
 * Ta funkcja `expect` ma różne implementacje dla każdej platformy (desktop, JS).
 *
 * @param name Nazwa pliku zasobu do znalezienia (np. "translations_pl.json").
 * @return Lista zawartości znalezionych plików jako String. Pusta lista jeśli nie znaleziono.
 */
expect fun loadResource(name: String): String?