package org.octavius.novels.form.control.type.repeatable

import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control

/**
 * Reprezentuje jeden wiersz w kontrolce powtarzalnej.
 * 
 * @param id unikalny identyfikator wiersza (generowany automatycznie)
 * @param states mapa stanów kontrolek w tym wierszu
 */
data class RepeatableRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val states: Map<String, ControlState<*>> = mapOf()
)

/**
 * Reprezentuje wynik kontrolki powtarzalnej z podziałem na typy operacji.
 * 
 * @param deletedRows wiersze do usunięcia z bazy danych
 * @param addedRows nowe wiersze do dodania
 * @param modifiedRows wiersze do zaktualizowania
 */
data class RepeatableResultValue(
    val deletedRows: List<Map<String, ControlResultData>>,
    val addedRows: List<Map<String,ControlResultData>>,
    val modifiedRows: List<Map<String,ControlResultData>>
)

/**
 * Tworzy nowy wiersz z pustymi stanami kontrolek.
 * 
 * @param rowControls mapa kontrolek które mają być w wierszu
 * @return nowy wiersz z zainicjalizowanymi stanami
 */
fun createRow(rowControls: Map<String, Control<*>>): RepeatableRow {
    val states = mutableMapOf<String, ControlState<*>>()

    rowControls.forEach { (name, control) ->
        states[name] = control.setInitValue(null)
    }

    return RepeatableRow(states = states)
}

/**
 * Analizuje stan kontrolki powtarzalnej i klasyfikuje wiersze według typu operacji.
 * 
 * @param controlState stan kontrolki powtarzalnej
 * @return Triple(nowe wiersze, usunięte wiersze, zmienione wiersze)
 */
fun getRowTypes(controlState: ControlState<List<RepeatableRow>>): Triple<List<RepeatableRow>, List<RepeatableRow>, List<RepeatableRow>> {
    val currentRowIds = controlState.value.value!!.map { it.id }.toSet()
    val initialRowIds = controlState.initValue.value!!.map { it.id }.toSet()

    // Nowe wiersze: te w current, których ID nie ma w initial
    val newRows = controlState.value.value!!.filter { it.id !in initialRowIds }

    // Usunięte wiersze: te w initial, których ID nie ma w current
    val deletedRows = controlState.initValue.value!!.filter { it.id !in currentRowIds }

    // Zmienione wiersze: te, które są w obu listach (wg ID) I mają przynajmniej jedno pole "dirty"
    val changedRows = controlState.value.value!!.filter { currentRow ->
        currentRow.id in initialRowIds && currentRow.states.values.any { it.dirty.value }
    }
    return Triple(newRows, deletedRows, changedRows)
}