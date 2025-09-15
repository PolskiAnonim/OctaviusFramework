package org.octavius.form.control.type.repeatable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormActionTrigger
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState
import org.octavius.form.control.base.*
import org.octavius.form.control.layout.repeatable.RepeatableHeader
import org.octavius.form.control.layout.repeatable.RepeatableRowCard
import org.octavius.form.control.layout.repeatable.RepeatableRowContent
import org.octavius.form.control.validator.repeatable.RepeatableValidator
import org.octavius.ui.theme.FormSpacing

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
    private lateinit var rowManager: RepeatableRowManager

    override fun setupFormReferences(
        formState: FormState,
        formSchema: FormSchema,
        errorManager: ErrorManager,
        formActionTrigger: FormActionTrigger
    ) {
        super.setupFormReferences(formState, formSchema, errorManager, formActionTrigger)
        rowControls.values.forEach { rowControl ->
            rowControl.setupFormReferences(formState, formSchema, errorManager, formActionTrigger)
        }
        this.rowManager = RepeatableRowManager(rowControls, formState, validationOptions as RepeatableValidation?)
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

    override fun setInitValue(controlName: String, value: Any?): ControlState<List<RepeatableRow>> {

        @Suppress("UNCHECKED_CAST")
        val initialDataRows = value as? List<Map<String, Any?>> ?: emptyList()

        // 1. Utwórz wiersze i stany dla danych początkowych
        val initialModelRows = initialDataRows.mapIndexed { index, dataRow ->
            val row = RepeatableRow(index = index)

            // Dla każdego pola w danych, stwórz stan kontrolki-dziecka
            rowControls.forEach { (fieldName, control) ->
                val fieldValue = dataRow[fieldName]
                val hierarchicalName = "$controlName[${row.id}].$fieldName"

                // Rekurencyjne wywołanie! Tworzymy stan dla dziecka.
                val childState = control.setInitValue(hierarchicalName, fieldValue)
                formState.setControlState(hierarchicalName, childState)
            }
            row
        }

        // 2. Dodaj puste wiersze, jeśli wymagane przez minItems
        val additionalModelRows = mutableListOf<RepeatableRow>()
        val minRows = (validationOptions as? RepeatableValidation)?.minItems ?: 0
        while (initialModelRows.size + additionalModelRows.size < minRows) {
            val index = initialModelRows.size + additionalModelRows.size

            // Używamy nowej funkcji pomocniczej
            val newRow = createRow(index, controlName, rowControls, formState)
            additionalModelRows.add(newRow)
        }

        // 3. Zwróć stan dla samego RepeatableControl
        val allInitialRows = initialModelRows + additionalModelRows
        return ControlState(
            initValue = mutableStateOf(initialModelRows),
            value = mutableStateOf(copyInitToValue(allInitialRows)),
        )
    }

    @Composable
    override fun Display(renderContext: RenderContext, controlState: ControlState<List<RepeatableRow>>, isRequired: Boolean) {
        Column(modifier = Modifier.fillMaxWidth()) {
            RepeatableHeader(
                label = label,
                onAddClick = {
                    rowManager.addRow(renderContext, controlState)
                },
                canAdd = rowManager.canAddRow(controlState)
            )

            Spacer(modifier = Modifier.height(FormSpacing.itemSpacing))

            controlState.value.value?.forEachIndexed { index, row ->
                RepeatableRowCard(
                    row = row,
                    index = index,
                    canDelete = rowManager.canDeleteRow(controlState),
                    onDelete = {
                        rowManager.deleteRow(renderContext, controlState, index)
                    },
                    content = {
                        RepeatableRowContent(
                            row = row,
                            renderContext = renderContext,
                            rowOrder = rowOrder,
                            rowControls = rowControls,
                            formState = formState
                        )
                    }
                )

                Spacer(modifier = Modifier.height(FormSpacing.itemSpacing))
            }
        }
    }

    override fun convertToResult(
        renderContext: RenderContext,
        state: ControlState<*>, // Ten state to ControlState<List<RepeatableRow>> dla RepeatableControl
    ): ControlResultData {
        @Suppress("UNCHECKED_CAST")
        val controlState = state as ControlState<List<RepeatableRow>>
        val states = formState.getAllStates()

        val (newRows, deletedRows, changedRows) = getRowTypes(
            controlState,
            renderContext,
            rowControls,
            states
        )

        val deletedRowsValues = deletedRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalContext = renderContext.forRepeatableChild(fieldName, row.id)
                val fieldState = states[hierarchicalContext.fullPath]!!
                control.getResult(hierarchicalContext, fieldState)
            }
        }

        val newRowsValues = newRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalContext = renderContext.forRepeatableChild(fieldName, row.id)
                val fieldControlState = states[hierarchicalContext.fullPath]!!
                control.getResult(hierarchicalContext, fieldControlState)
            }
        }

        val changedRowsValues = changedRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalContext = renderContext.forRepeatableChild(fieldName, row.id)
                val fieldControlState = states[hierarchicalContext.fullPath]!!
                control.getResult(hierarchicalContext, fieldControlState)
            }
        }

        val allCurrentRowsValues = controlState.value.value!!.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalContext = renderContext.forRepeatableChild(fieldName, row.id)
                val fieldControlState = states[hierarchicalContext.fullPath]!!
                control.getResult(hierarchicalContext, fieldControlState)
            }
        }

        // Wyczyść stany usuniętych wierszy które były oryginalne
        deletedRows.forEach { row ->
            val rowPrefix = "${renderContext.fullPath}[${row.id}]"
            formState.removeControlStatesWithPrefix(rowPrefix)
        }

        return ControlResultData(
            currentValue = RepeatableResultValue(
                deletedRows = deletedRowsValues,
                addedRows = newRowsValues,
                modifiedRows = changedRowsValues,
                allCurrentRows = allCurrentRowsValues
            ),
            initialValue = null  // Oryginalne wartości są już zawarte w currentValue dla poszczególnych kontrolek
        )
    }
}