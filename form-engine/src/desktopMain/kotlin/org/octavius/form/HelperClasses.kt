package org.octavius.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.octavius.navigation.Screen


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
 * @param revision licznik zmian z zewnątrz, służący do wymuszenia synchronizacji UI.
 * Nie musi być używany w prostych kontrolkach w których parsowanie wartości nie jest potrzebne
 */
data class ControlState<T>(
    val value: MutableState<T?> = mutableStateOf(null),
    val initValue: MutableState<T?> = mutableStateOf(null),
    val revision: MutableState<Int> = mutableStateOf(0)
)

/**
 * Definicja relacji między tabelami w zapytaniach JOIN dla formularzy.
 *
 * Data class opisująca jak tabele powinny być połączone w zapytaniach SQL
 * generowanych przez system formularzy. Umożliwia definiowanie złożonych
 * relacji między tabelami główną a tabelami powiązanymi.
 *
 * @param tableName Nazwa tabeli w relacji
 * @param joinCondition Warunek JOIN SQL (pusty string dla tabeli głównej)
 * @param primaryKey Nazwa kolumny klucza głównego (domyślnie "id")
 *
 * Przykład:
 * ```kotlin
 * TableRelation("users") // Tabela główna
 * TableRelation("profiles", "LEFT JOIN profiles p ON p.user_id = users.id")
 * ```
 */
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",
    val primaryKey: String = "id"
)

sealed class FormActionResult {
    // Akcje zmieniające UI
    data class Navigate(val screen: Screen) : FormActionResult() // Przekierowanie
    object CloseScreen : FormActionResult() // Akcja "Anuluj"
    //Akcje generyczne
    object ValidationFailed : FormActionResult() // Błędy walidacji
    object Failure : FormActionResult() // Ogólny błąd
    object Success : FormActionResult() // Generyczny sukces, np. po zapisie
}

interface FormActionTrigger {
    fun triggerAction(actionKey: String, validates: Boolean): FormActionResult
}