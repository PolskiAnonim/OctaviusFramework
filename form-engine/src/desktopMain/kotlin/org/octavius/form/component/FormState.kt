package org.octavius.form.component

import androidx.compose.runtime.mutableStateMapOf
import org.octavius.form.control.base.ControlResultData
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.FormResultData

/**
 * Klasa zarządzająca stanem formularza oraz kontrolek w jego wnętrzu
 * Używa mutableStateMapOf dla automatycznej rekomposycji przy zmianach
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
     * Funkcja inicjalizuje stany kontrolek na podstawie wartości inicjalnych
     */
    internal fun initializeStates(schema: FormSchema, initValues: Map<String, Any?>, errorManager: ErrorManager) {
        // Potem inicjalizuj stany kontrolek
        schema.getAllControls().forEach { (controlName, control) ->
            val value = initValues[controlName]
            _controlStates[controlName] = control.setInitValue(value)
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
     * Funkcja zwraca rezultat formularza w formie danych przetworzonych przez kontrolki
     */
    internal fun collectFormData(schema: FormSchema): FormResultData {
        val result = mutableMapOf<String, ControlResultData>()

        schema.getAllControls().forEach { (controlName, control) ->
            val state = _controlStates[controlName]!! // Stan musi istnieć, jeśli kontrolka jest w schemacie
            result[controlName] = control.getResult(controlName, state)
        }

        return result.toMap()
    }
}