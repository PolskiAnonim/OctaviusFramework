package org.octavius.form.control.type.repeatable

import org.octavius.form.component.FormState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.RenderContext
import org.octavius.form.control.base.RepeatableValidation

class RepeatableRowManager(
    private val rowControls: Map<String, Control<*>>,
    private val formState: FormState,
    private val validationOptions: RepeatableValidation?
) {
    
    fun addRow(renderContext: RenderContext, controlState: ControlState<List<RepeatableRow>>) {
        val currentRows = controlState.value.value!!.toMutableList()
        val newIndex = currentRows.size
        val newRow = createRow(newIndex, renderContext.fullPath, rowControls, formState)
        currentRows.add(newRow)
        controlState.value.value = currentRows
    }
    
    fun deleteRow(
        renderContext: RenderContext,
        controlState: ControlState<List<RepeatableRow>>,
        index: Int
    ) {
        val minRows = validationOptions?.minItems ?: 0
        if (controlState.value.value!!.size <= minRows) {
            return
        }
        
        val currentRows = controlState.value.value!!.toMutableList()
        val rowToRemove = currentRows[index]
        
        // Sprawdź czy wiersz był w oryginalnych danych
        val wasOriginal = controlState.initValue.value!!.any { it.id == rowToRemove.id }
        
        if (!wasOriginal) {
            // Nowy wiersz - usuń stany od razu
            formState.removeControlStatesWithPrefix("${renderContext.fullPath}[${rowToRemove.id}]")
        }
        
        // Usuń wiersz z listy
        currentRows.removeAt(index)
        
        // Zaktualizuj indeksy
        currentRows.forEachIndexed { newIndex, row ->
            currentRows[newIndex] = row.copy(index = newIndex)
        }
        
        controlState.value.value = currentRows
        return
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