package org.octavius.form.control.validator.repeatable

import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.RepeatableValidation
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.repeatable.RepeatableRow
import org.octavius.localization.Translations

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
     */
    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        @Suppress("UNCHECKED_CAST")
        val rows = state.value.value as List<RepeatableRow>

        val control = formSchema.getControl(controlName) as? RepeatableControl ?: return
        val allStates = formState.getAllStates()

        // 1. Walidacja kontrolek-dzieci
        for (row in rows) {
            for ((fieldName, fieldControl) in control.rowControls) {
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldState = allStates[hierarchicalName]

                fieldControl.validateControl(hierarchicalName, fieldState)
            }
        }

        // 2. Walidacja reguł listy
        val errors = mutableListOf<String>()
        validationOptions?.let { options ->
            // Sprawdź minimalną liczbę elementów
            options.minItems?.let { minItems ->
                if (rows.size < minItems) {
                    errors.add(Translations.get("validation.minItems", minItems))
                }
            }

            // Sprawdź maksymalną liczbę elementów
            options.maxItems?.let { maxItems ->
                if (rows.size > maxItems) {
                    errors.add(Translations.get("validation.maxItems", maxItems))
                }
            }

            // Sprawdź unikalność
            if (options.uniqueFields.isNotEmpty()) {
                val seenValues = mutableSetOf<List<Any?>>()
                for ((index, row) in rows.withIndex()) {
                    // Bierzemy pod uwagę tylko wiersze, które nie mają błędów w polach unikalności
                    val uniqueKey = options.uniqueFields.map { field ->
                        val hierarchicalName = "$controlName[${row.id}].$field"
                        allStates[hierarchicalName]?.value?.value
                    }

                    // Sprawdzamy, czy którekolwiek z pól klucza jest puste - takie klucze ignorujemy
                    if (uniqueKey.any { it == null || (it is String && it.isBlank()) }) {
                        continue
                    }

                    if (!seenValues.add(uniqueKey)) {
                        errors.add(Translations.get("validation.duplicateInRow", index + 1))
                        // Nie przerywamy, aby znaleźć wszystkie duplikaty, ale możemy dodać błąd tylko raz
                        // Można to udoskonalić, by wskazywać wszystkie zduplikowane wiersze. Na razie `break` jest OK.
                        break
                    }
                }
            }
        }

        // Ustawiamy błędy dla GŁÓWNEJ kontrolki
        errorManager.setFieldErrors(controlName, errors)
    }
}