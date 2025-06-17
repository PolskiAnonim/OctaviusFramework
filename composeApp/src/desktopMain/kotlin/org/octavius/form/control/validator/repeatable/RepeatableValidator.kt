package org.octavius.form.control.validator.repeatable

import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.RepeatableValidation
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.repeatable.RepeatableRow

/**
 * Walidator dla kontrolek typu RepeatableControl.
 *
 * Odpowiada za walidację kontrolek umożliwiających dodawanie
 * wielu wierszy danych (np. lista autorów, tagów, kategorii).
 *
 * Funkcjonalności:
 * - Sprawdzanie unikalności wartości w określonych polach
 * - Walidacja minimalnej i maksymalnej liczby elementów
 */
class RepeatableValidator(
    private val validationOptions: RepeatableValidation? = null
) : ControlValidator<List<RepeatableRow>>() {

    /**
     * Waliduje unikalność wartości w wierszach kontrolki powtarzalnej.
     * Używa globalnego stanu zamiast lokalnych stanów wierszy.
     *
     * Algorytm walidacji:
     * 1. Pobiera wszystkie wiersze z kontrolki
     * 2. Dla każdego wiersza tworzy klucz unikalności z wartości określonych pól
     * 3. Sprawdza czy klucz nie został już wcześniej napotkany
     * 4. Jeśli znajdzie duplikat, ustawia błąd z numerem wiersza
     *
     * Sprawdzanie odbywa się tylko gdy lista uniqueFields nie jest pusta.
     *
     * @param controlName nazwa kontrolki (potrzebna do budowania hierarchicznych nazw)
     * @param state stan kontrolki powtarzalnej zawierający listę wierszy
     * @param control referencja do kontrolki (RepeatableControl)
     * @param controls mapa wszystkich kontrolek
     * @param states mapa wszystkich stanów formularza
     */
    override fun validate(
        controlName: String,
        state: ControlState<*>,
        control: Control<*>
    ) {
        val states = formState.getAllStates()
        formSchema.getAllControls()

        @Suppress("UNCHECKED_CAST")
        val rows = state.value.value as List<RepeatableRow>

        @Suppress("UNCHECKED_CAST")
        val repeatableControl = control as RepeatableControl

        // Najpierw waliduj kontrolki wewnątrz wierszy
        for (row in rows) {
            for ((fieldName, fieldControl) in repeatableControl.rowControls) {
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldState = states[hierarchicalName]
                if (fieldState != null) {
                    fieldControl.validateControl(hierarchicalName, fieldState)
                }
            }
        }

        validationOptions?.let { options ->
            val errors = mutableListOf<String>()

            // Sprawdź minimalną liczbę elementów
            options.minItems?.let { minItems ->
                if (rows.size < minItems) {
                    errors.add("Wymagane minimum $minItems elementów")
                }
            }

            // Sprawdź maksymalną liczbę elementów
            options.maxItems?.let { maxItems ->
                if (rows.size > maxItems) {
                    errors.add("Maksymalnie $maxItems elementów")
                }
            }

            // Sprawdź unikalność
            if (options.uniqueFields.isNotEmpty()) {
                val seenValues = mutableSetOf<List<Any?>>()

                for ((index, row) in rows.withIndex()) {
                    val uniqueKey = options.uniqueFields.map { field ->
                        val hierarchicalName = "$controlName[${row.id}].$field"
                        states[hierarchicalName]?.value?.value
                    }

                    if (!seenValues.add(uniqueKey)) {
                        errors.add("Duplikat wartości w wierszu ${index + 1}")
                        break
                    }
                }
            }

            errorManager.setFieldErrors(controlName, errors)
            return
        }

        // Wyczyść błąd jeśli walidacja przeszła
        errorManager.setFieldErrors(controlName, emptyList())
    }

    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        // Ta metoda nie jest używana - używamy nadpisanej validate() z dodatkowymi parametrami
    }
}