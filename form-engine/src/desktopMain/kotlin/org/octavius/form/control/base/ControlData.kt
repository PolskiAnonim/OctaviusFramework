package org.octavius.form.control.base

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Wynik przetwarzania pojedynczej kontrolki formularza.
 *
 * Zawiera zarówno bieżącą wartość (zmodyfikowaną przez użytkownika)
 * jak i wartość początkową (z bazy lub domyślną) dla porównań.
 *
 * @param currentValue Bieżąca wartość kontrolki przetworzona przez getResult.
 * @param initialValue Pierwotna wartość załadowana z bazy lub ustawiona domyślnie.
 */
data class ControlResultData(
    val currentValue: Any?,
    val initialValue: Any?
)

/**
 * Reaktywny stan pojedynczej kontrolki formularza.
 *
 * Przechowuje wszystkie informacje o stanie kontrolki potrzebne
 * do renderowania, walidacji i śledzenia zmian, wykorzystując
 * Compose State API dla automatycznej rekomposycji.
 *
 * @param T Typ danych przechowywanych przez kontrolkę.
 * @param value Bieżąca wartość kontrolki (edytowana przez użytkownika) - MutableState.
 * @param initValue Pierwotna wartość załadowana z bazy lub ustawiona domyślnie - MutableState.
 * @param revision Licznik rewizji do wymuszenia synchronizacji UI w złożonych kontrolkach.
 *                 Używany gdy parsowanie wartości wymaga dodatkowej logiki.
 */
data class ControlState<T>(
    val value: MutableState<T?> = mutableStateOf(null),
    val initValue: MutableState<T?> = mutableStateOf(null),
    val revision: MutableState<Int> = mutableStateOf(0)
)

data class RenderContext(
    val localName: String, // "status", "publications", "subtasks"
    val basePath: String = ""  // "projects[uuid1]", "projects[uuid1].tasks[uuid2]"
) {
    // To jest teraz pełna, globalnie unikalna ścieżka do stanu w FormState
    val fullPath: String by lazy {
        if (basePath.isEmpty()) localName else "$basePath.$localName"
    }

    // Tworzy nowy, zagnieżdżony kontekst dla kontrolki-dziecka
    fun forChild(childLocalName: String): RenderContext {
        return RenderContext(
            localName = childLocalName,
            basePath = this.fullPath
        )
    }

    fun forRepeatableChild(childLocalName: String, rowId: String): RenderContext {
        // Specjalna wersja dla wierszy w RepeatableControl
        return RenderContext(localName = childLocalName, basePath = "${this.fullPath}[$rowId]")
    }
}

/**
 * Alias dla mapy zawierającej wyniki wszystkich kontrolek formularza.
 *
 * Kluczem jest nazwa kontrolki, wartością ControlResultData z bieżącą
 * i początkową wartością. Używane jako główny typ danych przekazywany
 * do logiki walidacji, zapisu i akcji formularza.
 */
typealias FormResultData = Map<String, ControlResultData>

/**
 * Pobiera bieżącą wartość kontrolki jako określony typ `T`.
 * Obsługuje rzutowanie na typ T
 *
 * Zastępuje: `formData["name"]?.currentValue as String`
 * Użycie: `formData.getCurrent<String>("name")`
 */
inline fun <reified T> FormResultData.getCurrentAs(key: String): T {
    return this[key]!!.currentValue as T
}

/**
 * Pobiera bieżącą wartość kontrolki jako Any?.
 *
 * Zastępuje: `formData["name"]!!.currentValue`
 */
fun FormResultData.getCurrent(key: String): Any? {
    return this[key]!!.currentValue
}

/**
 * Pobiera początkową wartość kontrolki jako określony typ `T`.
 *
 * Zastępuje: `rowData["id"]?.initialValue!! as Int`
 * Użycie: `rowData.getInitial<Int>("id")`
 */
inline fun <reified T> FormResultData.getInitialAs(key: String): T {
    return this[key]!!.initialValue as T
}

/**
 * Pobiera początkową wartość kontrolki jako Any?
 *
 * Zastępuje: `rowData["id"]?.initialValue as? Int`
 * Użycie: `rowData.getInitial<Int>("id")`
 */
fun FormResultData.getInitial(key: String): Any? {
    return this[key]!!.currentValue
}