package org.octavius.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Kontrakt dla zdarzeń nawigacyjnych, które mogą być wywołane z zewnątrz (np. przez API).
 */
sealed interface NavigationEvent {
    /** Zdarzenie żądania przełączenia na konkretną zakładkę. */
    data class SwitchToTab(val tabIndex: UShort) : NavigationEvent

    data class NavigateToScreen(val screenId: String, val payload: Map<String, Any>?) : NavigationEvent
}

/**
 * Globalna, bezpieczna wątkowo magistrala zdarzeń do komunikacji między-modułowej.
 */
object NavigationEventBus {
    private val _events = MutableSharedFlow<NavigationEvent>()
    val events = _events.asSharedFlow()

    suspend fun post(event: NavigationEvent) {
        _events.emit(event)
    }
}