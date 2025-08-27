package org.octavius.ui.error

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.octavius.exception.DatabaseException
import org.octavius.localization.Translations

data class ErrorDetails(val title: String, val message: String)


/**
 * Centralny singleton do zarządzania globalnymi, krytycznymi błędami.
 * Działa na zasadzie obserwowalnego stanu, który jest odczytywany przez główny
 * komponent UI (MainScreen) w celu wyświetlenia modala z błędem.
 */
object GlobalErrorHandler {
    private val _errorDetails = MutableStateFlow<ErrorDetails?>(null)

    /**
     * Reaktywny stan błędu obserwowany przez UI.
     * Emituje `null` gdy nie ma błędu, lub `ErrorDetails` gdy błąd ma być pokazany.
     */
    val errorDetails = _errorDetails.asStateFlow()

    /**
     * Wyświetla dialog błędu z podanym tytułem i wiadomością.
     */
    fun showError(title: String, message: String) {
        _errorDetails.update { ErrorDetails(title, message) }
    }

    /**
     * Wyświetla dialog błędu na podstawie wyjątku z bazy danych.
     */
    fun showError(error: DatabaseException) {
        val title = Translations.get("error.database.title")
        _errorDetails.update { ErrorDetails(title, error.toString()) }
    }

    /**
     * Zamyka dialog błędu. Wywoływane przez UI po interakcji użytkownika.
     */
    fun dismissError() {
        _errorDetails.update { null }
    }
}