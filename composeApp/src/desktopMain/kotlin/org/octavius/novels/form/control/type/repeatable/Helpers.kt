package org.octavius.novels.form.control.type.repeatable

import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control

data class RepeatableRow(
    // Id wiersza do wewnętrznego sprawdzania
    val id: String = java.util.UUID.randomUUID().toString(),
    // Stan kontrolek wiersza
    val states: Map<String, ControlState<*>> = mapOf()
)

data class RepeatableResultValue(
    val deletedRows: List<Map<String,Any?>>,
    val addedRows: List<Map<String,Any?>>,
    val modifiedRows: List<Map<String,Any?>>
)

fun createRow(rowControls: Map<String, Control<*>>): RepeatableRow {
    val states = mutableMapOf<String, ControlState<*>>()

    rowControls.forEach { (name, control) ->
        states[name] = control.setInitValue(null)
    }

    return RepeatableRow(states = states)
}

fun getRowTypes(controlState: ControlState<List<RepeatableRow>>): Triple<List<RepeatableRow>, List<RepeatableRow>, List<RepeatableRow>> {
    val currentRowIds = controlState.value.value!!.map { it.id }.toSet()
    val initialRowIds = controlState.initValue.value!!.map { it.id }.toSet()

    // Nowe wiersze: te w current, których ID nie ma w initial
    val newRows = controlState.value.value!!.filter { it.id !in initialRowIds }

    // Usunięte wiersze: te w initial, których ID nie ma w current
    // Potrzebujemy dostępu do ich `ControlState`ów pól, aby przekazać do getResult
    val deletedRows = controlState.initValue.value!!.filter { it.id !in currentRowIds }

    // Zmienione wiersze: te, które są w obu listach (wg ID) I mają przynajmniej jedno pole "dirty"
    val changedRows = controlState.value.value!!.filter { currentRow ->
        currentRow.id in initialRowIds && currentRow.states.values.any { it.dirty.value }
    }
    return Triple(newRows, deletedRows, changedRows)
}