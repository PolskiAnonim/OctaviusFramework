package org.octavius.contract

import io.ktor.server.routing.Routing

/**
 * Interfejs dla modułów, które chcą dodać własne ścieżki do głównego serwera Ktor.
 *
 * Każdy moduł `feature-*` może zaimplementować ten interfejs, aby zarejestrować
 * swoje endpointy API.
 */
interface ApiModule {
    /**
     * Instaluje trasy (routes) dla danego modułu w głównym obiekcie Routing Ktora.
     * @param routing Kontekst routingu z głównej aplikacji Ktor, do którego należy dodać ścieżki.
     */
    fun installRoutes(routing: Routing)
}