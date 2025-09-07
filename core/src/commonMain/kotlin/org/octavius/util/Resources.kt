package org.octavius.util

/**
 * Ładuje zawartość wszystkich plików o podanej nazwie z zasobów aplikacji.
 *
 * Ta funkcja `expect` ma różne implementacje dla każdej platformy (desktop, JS).
 * Przeszukuje wszystkie dostępne zasoby w classpath i zwraca zawartość
 * wszystkich pasujących plików.
 *
 * @param name Nazwa pliku zasobu do znalezienia (np. "translations_pl.json").
 * @return Lista zawartości znalezionych plików jako String. Pusta lista jeśli nie znaleziono.
 */
expect fun loadResources(name: String): List<String>