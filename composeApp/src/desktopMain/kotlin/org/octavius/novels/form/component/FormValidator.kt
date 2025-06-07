package org.octavius.novels.form.component

import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control

/**
 * Klasa odpowiedzialna za walidację formularza na dwóch poziomach:
 * 1. Walidacja pól - sprawdza wymagalność, format, zależności między kontrolkami
 * 2. Walidacja reguł biznesowych - sprawdza niestandardowe reguły specyficzne dla domeny
 *
 * Klasa może być rozszerzona dla implementacji niestandardowych reguł walidacji.
 */
open class FormValidator(protected val errorManager: ErrorManager) {
    /**
     * Waliduje wszystkie pola formularza.
     *
     * Proces walidacji:
     * 1. Czyści poprzednie błędy
     * 2. Uruchamia walidację każdej kontrolki przez jej validator
     * 3. Sprawdza wymagalność, format, zależności
     *
     * @param controls mapa wszystkich kontrolek formularza
     * @param states mapa stanów wszystkich kontrolek
     * @return true jeśli wszystkie pola są poprawne
     */
    fun validateFields(controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Boolean {
        // Wyczyść poprzednie błędy pól
        errorManager.clearFieldErrors()
        
        for ((controlName, control) in controls) {
            val state = states[controlName]!!
            control.validateControl(controlName, state)
        }

        // Sprawdź czy są jakieś błędy pól
        return !errorManager.hasFieldErrors()
    }

    /**
     * Waliduje reguły biznesowe specyficzne dla domeny.
     *
     * Domyślna implementacja zawsze zwraca true.
     * Klasy pochodne powinny przesłonić tę metodę dla implementacji
     * niestandardowych reguł walidacji (np. sprawdzanie duplikatów,
     * weryfikacja relacji między polami, itp.)
     *
     * @param formData zebrane dane z formularza
     * @return true jeśli reguły biznesowe są spełnione
     */
    open fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        return true
    }

}