package org.octavius.dialog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralny singleton do zarządzania globalnymi dialogami w aplikacji.
 * Działa na zasadzie obserwowalnego stanu, który jest odczytywany przez główny
 * komponent UI (MainScreen) w celu wyświetlenia modala.
 */
object GlobalDialogManager {
    private val _dialogConfig = MutableStateFlow<DialogConfig?>(null)

    /**
     * Reaktywny stan dialogu obserwowany przez UI.
     * Emituje `null` gdy nie ma dialogu, lub `DialogConfig` gdy dialog ma być pokazany.
     */
    val dialogConfig = _dialogConfig.asStateFlow()

    /**
     * Wyświetla globalny dialog na podstawie podanej konfiguracji.
     */
    fun show(config: DialogConfig) {
        _dialogConfig.value = config
    }

    /**
     * Zamyka aktualnie wyświetlany dialog.
     */
    fun dismiss() {
        _dialogConfig.value = null
    }
}