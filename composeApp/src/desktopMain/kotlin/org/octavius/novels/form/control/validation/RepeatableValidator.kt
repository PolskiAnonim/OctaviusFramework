package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.type.repeatable.RepeatableRow

/**
 * Walidator dla kontrolek typu RepeatableControl.
 * 
 * Odpowiada za walidację kontrolek umożliwiających dodawanie
 * wielu wierszy danych (np. lista autorów, tagów, kategorii).
 * 
 * Główną funkcjonalnością jest sprawdzanie unikalności wartości
 * w określonych polach - zapobiega dodawaniu duplikatów
 * w polach które muszą być unikalne.
 * 
 * @param uniqueFields lista nazw pól, które muszą być unikalne w ramach kontrolki
 */
class RepeatableValidator(
    private val uniqueFields: List<String>
) : ControlValidator<List<RepeatableRow>>() {

    /**
     * Waliduje unikalność wartości w wierszach kontrolki powtarzalnej.
     * 
     * Algorytm walidacji:
     * 1. Pobiera wszystkie wiersze z kontrolki
     * 2. Dla każdego wiersza tworzy klucz unikalności z wartości określonych pól
     * 3. Sprawdza czy klucz nie został już wcześniej napotkany
     * 4. Jeśli znajdzie duplikat, ustawia błąd z numerem wiersza
     * 
     * Sprawdzanie odbywa się tylko gdy lista uniqueFields nie jest pusta.
     * 
     * @param state stan kontrolki powtarzalnej zawierający listę wierszy
     */
    override fun validateSpecific(state: ControlState<*>) {
        @Suppress("UNCHECKED_CAST")
        val rows = state.value.value as List<RepeatableRow>

        // Sprawdź unikalność
        if (uniqueFields.isNotEmpty()) {
            val seenValues = mutableSetOf<List<Any?>>()

            for ((index, row) in rows.withIndex()) {
                val uniqueKey = uniqueFields.map { field -> row.states[field]!!.value.value }

                if (!seenValues.add(uniqueKey)) {
                    state.error.value = "Duplikat wartości w wierszu ${index + 1}"
                    return
                }
            }
        }
    }
}