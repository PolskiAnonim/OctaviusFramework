package org.octavius.form.component

import androidx.compose.runtime.mutableStateMapOf
import org.octavius.form.control.base.ControlResultData
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.ControlContext

/**
 * Reaktywne zarządzanie stanem wszystkich kontrolek formularza.
 *
 * FormState przechowuje i zarządza stanami wszystkich kontrolek w formularzu,
 * wykorzystując Compose State API dla automatycznej rekomposycji UI przy zmianach.
 *
 * Główne funkcje:
 * - Przechowywanie reaktywnych stanów kontrolek (MutableState)
 * - Automatyczne wyzwalanie rekomposycji przy zmianach
 * - Zarządzanie hierarchicznymi nazwami kontrolek (np. dla RepeatableControl)
 * - Zbieranie danych z wszystkich kontrolek do przetworzenia
 *
 * Stany kontrolek są indeksowane pełną ścieżką (np. "publications[uuid].title"),
 * co umożliwia obsługę złożonych, zagnieżdżonych struktur.
 */
class FormState {
    /**
     * Stan kontrolek formularza - reactive map która automatycznie triggeruje recomposition
     */
    private val _controlStates = mutableStateMapOf<String, ControlState<*>>()

    /**
     * Funkcja zwraca stan kontrolki o danej nazwie
     */
    fun getControlState(name: String): ControlState<*>? = _controlStates[name]

    /**
     * Funkcja zwraca stany wszystkich kontrolek formularza
     */
    fun getAllStates(): Map<String, ControlState<*>> = _controlStates

    /**
     * Inicjalizuje stany wszystkich kontrolek na podstawie wartości początkowych.
     *
     * Dla każdej kontrolki ze schematu:
     * 1. Pobiera odpowiednią wartość z mapy inicjalnych
     * 2. Wywołuje setInitValue() na kontrolce
     * 3. Zapisuje utworzony stan w mapie reaktywnej
     *
     * @param schema Schemat formularza z definicjami kontrolek.
     * @param initValues Mapa wartości początkowych (klucz = nazwa kontrolki).
     */
    internal fun initializeStates(schema: FormSchema, initValues: Map<String, Any?>) {
        // Potem inicjalizuj stany kontrolek
        schema.getMainLevelControls().forEach { (controlName, control) ->
            val value = initValues[controlName]
            _controlStates[controlName] = control.setInitValue(controlName, value)
        }
    }
    /**
     * Funkcja ustawia stan kontrolki o danej nazwie (może być hierarchiczna)
     * Automatycznie triggeruje recomposition dzięki mutableStateMapOf
     */
    internal fun setControlState(name: String, state: ControlState<*>) {
        _controlStates[name] = state
    }

    /**
     * Funkcja usuwa stan kontrolki o danej nazwie
     * Automatycznie triggeruje recomposition dzięki mutableStateMapOf
     */
    internal fun removeControlState(name: String) {
        _controlStates.remove(name)
    }

    /**
     * Funkcja usuwa wszystkie stany kontrolek zaczynające się od danego prefiksu
     * Automatycznie triggeruje recomposition dzięki mutableStateMapOf
     */
    internal fun removeControlStatesWithPrefix(prefix: String) {
        val keysToRemove = _controlStates.keys.filter { it.startsWith(prefix) }
        keysToRemove.forEach { _controlStates.remove(it) }
    }

    /**
     * Zbiera i przetwarza dane ze wszystkich kontrolek formularza.
     *
     * Dla każdej kontrolki ze schematu:
     * 1. Pobiera jej aktualny stan
     * 2. Wywołuje getResult() na kontrolce
     * 3. Zwraca mapę z wynikami gotowymi do walidacji i zapisu
     *
     * @param schema Schemat formularza z definicjami kontrolek.
     * @return FormResultData - mapa wyników (klucz = nazwa kontrolki, wartość = ControlResultData).
     */
    internal fun collectFormData(schema: FormSchema): FormResultData {
        val result = mutableMapOf<String, ControlResultData>()

        schema.getMainLevelControls().forEach { (controlName, control) ->
            val state = _controlStates[controlName]!! // Stan musi istnieć, jeśli kontrolka jest w schemacie
            result[controlName] = control.getResult(ControlContext(controlName), state)
        }

        return result.toMap()
    }
}