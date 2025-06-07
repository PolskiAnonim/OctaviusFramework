package org.octavius.novels.form.control.type.repeatable

import org.octavius.novels.form.ControlState
import org.octavius.novels.form.component.FormState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.validation.RepeatableValidation

class RepeatableRowManager(
    private val controlName: String,
    private val rowControls: Map<String, Control<*>>,
    private val formState: FormState,
    private val validationOptions: RepeatableValidation?
) {
    
    fun addRow(controlState: ControlState<List<RepeatableRow>>) {
        val currentRows = controlState.value.value!!.toMutableList()
        val newIndex = currentRows.size
        val newRow = createRow(newIndex, controlName, rowControls, formState)
        currentRows.add(newRow)
        controlState.value.value = currentRows
        controlState.dirty.value = true
    }
    
    fun deleteRow(
        controlState: ControlState<List<RepeatableRow>>,
        index: Int
    ): Boolean {
        val minRows = validationOptions?.minItems ?: 0
        if (controlState.value.value!!.size <= minRows) {
            return false
        }
        
        val currentRows = controlState.value.value!!.toMutableList()
        val rowToRemove = currentRows[index]
        
        // Sprawdź czy wiersz był w oryginalnych danych
        val wasOriginal = controlState.initValue.value!!.any { it.id == rowToRemove.id }
        
        if (!wasOriginal) {
            // Nowy wiersz - usuń stany od razu
            formState.removeControlStatesWithPrefix("$controlName[${rowToRemove.id}]")
        }
        
        // Usuń wiersz z listy
        currentRows.removeAt(index)
        
        // Zaktualizuj indeksy
        currentRows.forEachIndexed { newIndex, row ->
            currentRows[newIndex] = row.copy(index = newIndex)
        }
        
        controlState.value.value = currentRows
        controlState.dirty.value = true
        return true
    }
    
    fun canAddRow(controlState: ControlState<List<RepeatableRow>>): Boolean {
        val maxRows = validationOptions?.maxItems
        return maxRows == null || controlState.value.value!!.size < maxRows
    }
    
    fun canDeleteRow(controlState: ControlState<List<RepeatableRow>>): Boolean {
        val minRows = validationOptions?.minItems ?: 0
        return controlState.value.value!!.size > minRows
    }
}