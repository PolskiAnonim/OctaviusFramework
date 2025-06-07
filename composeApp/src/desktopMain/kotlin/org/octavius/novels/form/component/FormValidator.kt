package org.octavius.novels.form.component

import org.octavius.novels.form.ControlResultData

/**
 * Klasa odpowiedzialna za walidację formularza na dwóch poziomach:
 * 1. Walidacja pól - sprawdza wymagalność, format, zależności między kontrolkami
 * 2. Walidacja reguł biznesowych - sprawdza niestandardowe reguły specyficzne dla domeny
 *
 * Klasa może być rozszerzona dla implementacji niestandardowych reguł walidacji.
 */
open class FormValidator() {

    protected var formState: FormState? = null
    protected var formSchema: FormSchema? = null
    protected var errorManager: ErrorManager? = null

    fun setupFormReferences(formState: FormState, formSchema: FormSchema, errorManager: ErrorManager) {
        this.formState = formState
        this.formSchema = formSchema
        this.errorManager = errorManager
    }

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
    fun validateFields(): Boolean {
        // Wyczyść poprzednie błędy pól
        errorManager!!.clearFieldErrors()

        for ((controlName, control) in formSchema!!.getAllControls()) {
            val state = formState!!.getControlState(controlName)!!
            control.validateControl(controlName, state)
        }

        // Sprawdź czy są jakieś błędy pól
        return !errorManager!!.hasFieldErrors()
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