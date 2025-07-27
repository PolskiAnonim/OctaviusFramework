package org.octavius.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Kontrakt dla zdarzeń nawigacyjnych, które mogą być wywołane z zewnątrz (np. przez API,
 * inny moduł).
 */
sealed interface NavigationEvent {
    /**
     * Zdarzenie żądania przełączenia na konkretną zakładkę.
     * @property tabIndex Indeks zakładki docelowej.
     */
    data class SwitchToTab(val tabIndex: UShort) : NavigationEvent

    /**
     * Zdarzenie żądania nawigacji do konkretnego ekranu w ramach stosu nawigacji
     * aktywnej zakładki.
     * @property screenId Unikalny identyfikator ekranu docelowego.
     * @property payload Opcjonalne dane do przekazania do ekranu docelowego.
     */
    data class NavigateToScreen(val screenId: String, val payload: Map<String, Any>?) : NavigationEvent
}

/**
 * Globalna, bezpieczna wątkowo magistrala zdarzeń do komunikacji między-modułowej.
 *
 * Umożliwia dowolnej części aplikacji zgłoszenie żądania zmiany nawigacji bez
 * posiadania bezpośredniej referencji do kontrolera nawigacji.
 */
object NavigationEventBus {
    private val _events = MutableSharedFlow<NavigationEvent>()
    /**
     * Publiczny strumień zdarzeń [NavigationEvent], który można subskrybować,
     * aby reagować na żądania nawigacyjne.
     */
    val events = _events.asSharedFlow()

    /**
     * Publikuje nowe zdarzenie [NavigationEvent] w magistrali.
     * @param event Zdarzenie nawigacyjne do wyemitowania.
     */
    suspend fun post(event: NavigationEvent) {
        _events.emit(event)
    }
}