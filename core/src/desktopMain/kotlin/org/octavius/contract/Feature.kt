package org.octavius.contract

/**
 * Interfejs reprezentujący kompletny, spójny moduł funkcjonalny ("Feature").
 *
 * Każdy moduł w aplikacji (np. Asian Media, Games) implementuje ten interfejs,
 * aby w jednym miejscu zadeklarować wszystkie swoje punkty integracji z główną aplikacją:
 * - Zakładkę w UI (`Tab`)
 * - Endpointy API (`ApiModule`)
 * - Ekrany dostępne z zewnątrz (`ScreenFactory`)
 *
 * Główna aplikacja (`desktop-app`) zbiera listę obiektów `FeatureModule` i automatycznie
 * konfiguruje na ich podstawie nawigację, serwer API i routing zdarzeń.
 */
interface FeatureModule {
    /** Nazwa modułu. */
    val name: String

    /**
     * Zwraca definicję zakładki UI dla tego modułu.
     * @return Obiekt `Tab` lub `null`, jeśli moduł nie ma reprezentacji w głównym menu.
     */
    fun getTab(): Tab?
}