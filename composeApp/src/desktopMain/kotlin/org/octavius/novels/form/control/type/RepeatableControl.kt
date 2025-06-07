package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.component.ErrorManager
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.type.repeatable.*
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.RepeatableValidation
import org.octavius.novels.form.control.validation.RepeatableValidator

/**
 * Kontrolka do tworzenia dynamicznych list kontrolek (wierszy).
 *
 * Umożliwia użytkownikowi dodawanie i usuwanie wierszy, gdzie każdy wiersz
 * zawiera zestaw kontrolek. Obsługuje walidację unikalności pól, ograniczenia
 * minimalnej i maksymalnej liczby wierszy oraz składanie/rozwijanie wierszy.
 * Każdy wiersz może być niezależnie edytowany i usuwany.
 */
class RepeatableControl(
    val rowControls: Map<String, Control<*>>,
    val rowOrder: List<String>,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: RepeatableValidation? = null
) : Control<List<RepeatableRow>>(
    label,
    null,
    required,
    dependencies,
    hasStandardLayout = false,
    validationOptions = validationOptions
) {

    private lateinit var controlName: String
    private lateinit var rowManager: RepeatableRowManager

    override fun setupFormReferences(
        formState: FormState,
        formSchema: FormSchema,
        errorManager: ErrorManager,
        controlName: String
    ) {
        super.setupFormReferences(formState, formSchema, errorManager, controlName)
        rowControls.values.forEach { rowControl ->
            rowControl.setupFormReferences(formState, formSchema, errorManager, "")
        }
        this.controlName = controlName
        this.rowManager = RepeatableRowManager(controlName, rowControls, formState, validationOptions as RepeatableValidation?)
    }

    override fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        // Ustaw parent dla wszystkich kontrolek w wierszu
        rowControls.values.forEach { childControl ->
            childControl.parentControl = parentControlName
        }
    }

    override val validator: ControlValidator<List<RepeatableRow>> = RepeatableValidator(
        validationOptions
    )

    override fun copyInitToValue(value: List<RepeatableRow>): List<RepeatableRow> {
        // Dla globalnego stanu nie trzeba kopiować stanów - są już w FormState
        return value.map { RepeatableRow(id = it.id, index = it.index) }
    }

    override fun setInitValue(value: Any?): ControlState<List<RepeatableRow>> {
        @Suppress("UNCHECKED_CAST")
        val initialRows = value as? List<Map<String, Any?>> ?: emptyList()

        // Utwórz wiersze i dodaj ich stany do globalnego FormState
        val initialRowsList = initialRows.mapIndexed { index, initialRow ->
            val row = RepeatableRow(index = index)

            // Dodaj stany kontrolek dla tego wiersza do globalnego FormState
            initialRow.forEach { (fieldName, fieldValue) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val control = rowControls[fieldName]!!
                val fieldState = control.setInitValue(fieldValue)
                formState.setControlState(hierarchicalName, fieldState)
            }

            row
        }

        val additionalRows = mutableListOf<RepeatableRow>()
        val minRows = (validationOptions as? RepeatableValidation)?.minItems ?: 0
        // Dodaj minimalne wiersze jeśli potrzeba
        while (initialRowsList.size + additionalRows.size < minRows) {
            val index = initialRowsList.size + additionalRows.size
            val newRow = createRow(index, controlName, rowControls, formState!!)
            additionalRows.add(newRow)
        }

        val state = ControlState(
            initValue = mutableStateOf(initialRowsList),
            value = mutableStateOf(copyInitToValue(initialRowsList) + additionalRows),
            dirty = mutableStateOf(true) // ta kontrolka zawsze jest dirty
        )
        return state
    }

    @Composable
    override fun Display(controlName: String, controlState: ControlState<List<RepeatableRow>>, isRequired: Boolean) {
        Column(modifier = Modifier.fillMaxWidth()) {
            RepeatableHeader(
                label = label,
                onAddClick = {
                    rowManager.addRow(controlState)
                    updateState(controlState)
                },
                canAdd = rowManager.canAddRow(controlState)
            )

            Spacer(modifier = Modifier.height(8.dp))

            controlState.value.value?.forEachIndexed { index, row ->
                RepeatableRowCard(
                    row = row,
                    index = index,
                    canDelete = rowManager.canDeleteRow(controlState),
                    onDelete = {
                        if (rowManager.deleteRow(controlState, index)) {
                            updateState(controlState)
                        }
                    },
                    content = {
                        RepeatableRowContent(
                            row = row,
                            controlName = controlName,
                            rowOrder = rowOrder,
                            rowControls = rowControls,
                            formState = formState
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    override fun convertToResult(
        state: ControlState<*>, // Ten state to ControlState<List<RepeatableRow>> dla RepeatableControl
    ): Any? {
        @Suppress("UNCHECKED_CAST")
        val controlState = state as ControlState<List<RepeatableRow>>
        val states = formState.getAllStates()
        // controlName jest teraz lateinit i zawsze będzie ustawiony

        val (newRows, deletedRows, changedRows) = getRowTypes(
            controlState,
            controlName,
            rowControls,
            states
        )

        val deletedRowsValues = deletedRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldControlState = states[hierarchicalName]!!
                val value = control.getResult(hierarchicalName, fieldControlState)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val newRowsValues = newRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldControlState = states[hierarchicalName]!!
                val value = control.getResult(hierarchicalName, fieldControlState)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val changedRowsValues = changedRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldControlState = states[hierarchicalName]!!
                val value = control.getResult(hierarchicalName, fieldControlState)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        // Wyczyść stany usuniętych wierszy które były oryginalne
        deletedRows.forEach { row ->
            formState.removeControlStatesWithPrefix("$controlName[${row.id}]")
        }

        return RepeatableResultValue(
            deletedRows = deletedRowsValues,
            addedRows = newRowsValues,
            modifiedRows = changedRowsValues
        )
    }
}