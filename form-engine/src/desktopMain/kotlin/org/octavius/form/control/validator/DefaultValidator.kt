package org.octavius.form.control.validator

import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.ControlValidator

/**
 * Domyślna implementacja walidatora kontrolek formularza.
 *
 * Używana dla kontrolek, które nie wymagają specjalnej walidacji
 * poza standardową walidacją wymagalności i widoczności
 * zaimplementowaną w klasie bazowej ControlValidator.
 *
 * Jest to implementacja "null object pattern" - zapewnia,
 * że każda kontrolka ma przypisany walidator, nawet jeśli
 * nie wykonuje on żadnej specyficznej walidacji.
 *
 * @param T typ danych przechowywanych przez kontrolkę
 */
class DefaultValidator<T : Any> : ControlValidator<T>() {
    /**
     * Nie wykonuje żadnej dodatkowej walidacji.
     *
     * Wszystkie podstawowe operacje walidacji (wymagalność, widoczność)
     * są obsługiwane przez klasę bazową ControlValidator.
     *
     * @param state stan kontrolki - nieużywany w tej implementacji
     */
    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        // Domyślna implementacja nie wykonuje dodatkowej walidacji
    }
}