package org.octavius.novels.form.component

import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState

/**
 * Klasa zarządzająca stanem formularza oraz kontrolek w jego wnętrzu
 */
class FormState {
    /**
     * Stan kontrolek fomularza
     */
    private val controlStates: MutableMap<String, ControlState<*>> = mutableMapOf()

    /**
     * Funkcja zwraca stan kontrolki o danej nazwie
     */
    fun getControlState(name: String): ControlState<*>? = controlStates[name]

    /**
     * Funkcja zwraca stany wszystkich kontrolek formularza
     */
    fun getAllStates(): Map<String, ControlState<*>> = controlStates.toMap()

    /**
     * Funkcja inicjalizuje stany kontrolek na podstawie wartości inicjalnych
     */
    fun initializeStates(schema: FormSchema, initValues: Map<String, Any?>) {
        schema.getAllControls().forEach { (controlName, control) ->
            val value = initValues[controlName]
            controlStates[controlName] = control.setInitValue(value)
        }
    }

    /**
     * Funkcja zwraca rezultat formularza w formie danych przetworzonych przez kontrolki
     */
    fun collectFormData(schema: FormSchema): Map<String, ControlResultData> {
        val result = mutableMapOf<String, ControlResultData>()

        schema.getAllControls().forEach { (controlName, control) ->
            val state = controlStates[controlName]!!
            val value = control.getResult(state, schema.getAllControls(), controlStates)
            result[controlName] = ControlResultData(value, state.dirty.value)
        }

        return result
    }
}