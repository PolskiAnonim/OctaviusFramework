package org.octavius.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf




/**
 * Dane wyniku kontrolki zebrane podczas przetwarzania formularza.
 *
 * @param currentValue wartość kontrolki przygotowana do zapisu w bazie danych
 * @param initialValue pierwotna wartość załadowana z bazy lub ustawiona domyślnie
 */
data class ControlResultData(
    val currentValue: Any?,
    val initialValue: Any?
)

/**
 * Stan pojedynczej kontrolki w formularzu.
 *
 * Przechowuje wszystkie informacje o stanie kontrolki potrzebne
 * do renderowania, walidacji i śledzenia zmian.
 *
 * @param T typ danych przechowywanych przez kontrolkę
 * @param value bieżąca wartość kontrolki (edytowana przez użytkownika)
 * @param initValue pierwotna wartość załadowana z bazy lub ustawiona domyślnie
 */
data class ControlState<T>(
    val value: MutableState<T?> = mutableStateOf(null),
    val initValue: MutableState<T?> = mutableStateOf(null)
)