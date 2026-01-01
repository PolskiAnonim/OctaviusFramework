package org.octavius.form.control.type.repeatable

import org.octavius.form.component.FormState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.ControlContext

/**
 * Reprezentuje jeden wiersz w kontrolce powtarzalnej.
 * Uproszczona wersja - stan jest teraz zarządzany globalnie.
 *
 * @param id unikalny identyfikator wiersza (generowany automatycznie)
 * @param index pozycja wiersza w liście (używana do budowania hierarchicznych nazw)
 */
data class RepeatableRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val index: Int = 0
)

/**
 * Reprezentuje wynik kontrolki powtarzalnej z podziałem na typy operacji.
 *
 * @param deletedRows wiersze do usunięcia z bazy danych
 * @param addedRows nowe wiersze do dodania
 * @param modifiedRows wiersze do zaktualizowania
 */
data class RepeatableResultValue(
    val deletedRows: List<FormResultData>,
    val addedRows: List<FormResultData>,
    val modifiedRows: List<FormResultData>,
    val allCurrentRows: List<FormResultData>
)

/**
 * Tworzy nowy wiersz i dodaje stany jego kontrolek do globalnego FormState.
 *
 * @param index pozycja wiersza w liście
 * @param parentControlName nazwa kontrolki powtarzalnej (do budowania hierarchicznych nazw)
 * @param rowControls mapa kontrolek które mają być w wierszu
 * @param formState globalny stan formularza
 * @return nowy wiersz z ustawionym indeksem
 */
internal fun createRow(
    index: Int,
    parentControlName: String, // Np. "publications" lub "projects[uuid].tasks"
    rowControls: Map<String, Control<*>>,
    formState: FormState
): RepeatableRow {
    val row = RepeatableRow(index = index)
    // Utwórz stany dla wszystkich kontrolek w wierszu
    rowControls.forEach { (fieldName, control) ->
        val hierarchicalName = "$parentControlName[${row.id}].$fieldName"
        val state = control.setInitValue(hierarchicalName, null) // Inicjalizujemy z null
        formState.setControlState(hierarchicalName, state)
    }
    return row
}

/**
 * Analizuje stan kontrolki powtarzalnej i klasyfikuje wiersze według typu operacji.
 * Używa globalnego stanu zamiast lokalnych stanów wierszy.
 *
 * @param controlState stan kontrolki powtarzalnej
 * @param controlContext kontekst kontrolki powtarzalnej (do budowania hierarchicznych nazw)
 * @param rowControls mapa kontrolek w wierszu
 * @param globalStates mapa wszystkich stanów formularza
 * @return Triple(nowe wiersze, usunięte wiersze, zmienione wiersze)
 */
internal fun getRowTypes(
    controlState: ControlState<List<RepeatableRow>>,
    controlContext: ControlContext,
    rowControls: Map<String, Control<*>>,
    globalStates: Map<String, ControlState<*>>
): Triple<List<RepeatableRow>, List<RepeatableRow>, List<RepeatableRow>> {
    val currentRowIds = controlState.value.value!!.map { it.id }.toSet()
    val initialRowIds = controlState.initValue.value!!.map { it.id }.toSet()

    // Nowe wiersze: te w current, których ID nie ma w initial
    val newRows = controlState.value.value!!.filter { it.id !in initialRowIds }

    // Usunięte wiersze: te w initial, których ID nie ma w current
    val deletedRows = controlState.initValue.value!!.filter { it.id !in currentRowIds }

    // Zmienione wiersze: te, które są w obu listach (wg ID) I mają przynajmniej jedno pole "dirty"
    val changedRows = controlState.value.value!!.filter { currentRow ->
        if (currentRow.id !in initialRowIds) return@filter false

        // Sprawdź czy jakiekolwiek pole w tym wierszu jest zmienione
        rowControls.keys.any { fieldName ->
            val hierarchicalContext = controlContext.forRepeatableChild(fieldName, currentRow.id)
            val state = globalStates[hierarchicalContext.fullStatePath]!!
            state.initValue.value != state.value.value
        }
    }
    return Triple(newRows, deletedRows, changedRows)
}