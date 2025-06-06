package org.octavius.novels.form.control.type

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.component.FormState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.RenderError
import org.octavius.novels.form.control.type.repeatable.RepeatableResultValue
import org.octavius.novels.form.control.type.repeatable.RepeatableRow
import org.octavius.novels.form.control.type.repeatable.createRow
import org.octavius.novels.form.control.type.repeatable.getRowTypes
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.RepeatableValidator
import org.octavius.novels.form.component.ErrorManager
import org.octavius.novels.form.component.FormSchema

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
    val uniqueFields: List<String> = emptyList(),
    val minRows: Int = 0,
    val maxRows: Int? = null,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<List<RepeatableRow>>(label, null, required, dependencies, hasStandardLayout = false) {

    // Referencja do nazwy kontrolki - będzie ustawiona przez setupFormReferences
    private var controlName: String? = null

    override fun setupFormReferences(
        formState: FormState,
        formSchema: FormSchema,
        errorManager: ErrorManager,
        controlName: String
    ) {
        super.setupFormReferences(formState, formSchema, errorManager, controlName)
        this.controlName = controlName
    }

    override fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        // Ustaw parent dla wszystkich kontrolek w wierszu
        rowControls.values.forEach { childControl ->
            childControl.parentControl = parentControlName
        }
    }

    override val validator: ControlValidator<List<RepeatableRow>> = RepeatableValidator(uniqueFields)

    override fun copyInitToValue(init: List<RepeatableRow>): List<RepeatableRow> {
        // Dla globalnego stanu nie trzeba kopiować stanów - są już w FormState
        return init.map { RepeatableRow(id = it.id, index = it.index) }
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
                control.setupFormReferences(formState!!, formSchema!!, errorManager!!, hierarchicalName)
                val fieldState = control.setInitValue(fieldValue)
                formState!!.setControlState(hierarchicalName, fieldState)
            }

            row
        }

        val additionalRows = mutableListOf<RepeatableRow>()

        // Dodaj minimalne wiersze jeśli potrzeba
        while (initialRowsList.size + additionalRows.size < minRows) {
            val index = initialRowsList.size + additionalRows.size
            val newRow = createRow(index, controlName!!, rowControls, formState!!)
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
    override fun Display(
        controlState: ControlState<List<RepeatableRow>>,
        isRequired: Boolean
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {
            // Nagłówek z przyciskiem dodawania
            RepeatableHeader(
                label = label,
                onAddClick = {
                    val currentRows = controlState.value.value!!.toMutableList()
                    val newIndex = currentRows.size
                    val newRow = createRow(newIndex, controlName!!, rowControls, formState!!)
                    currentRows.add(newRow)
                    controlState.value.value = currentRows
                    updateState(controlState)
                },
                canAdd = maxRows == null || (controlState.value.value!!.size) < maxRows
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista wierszy
            controlState.value.value?.forEachIndexed { index, row ->
                RepeatableRowCard(
                    row = row,
                    index = index,
                    onDelete = if ((controlState.value.value!!.size) > minRows) {
                        {
                            val currentRows = controlState.value.value!!.toMutableList()
                            val rowToRemove = currentRows[index]

                            // Sprawdź czy wiersz był w oryginalnych danych
                            val wasOriginal = controlState.initValue.value!!.any { it.id == rowToRemove.id }

                            if (wasOriginal) {
                                // Istniejący wiersz - zostaw stany dla convertToResult
                                // (będą usunięte później w convertToResult)
                            } else {
                                // Nowy wiersz - usuń stany od razu, bo nie potrzebujemy ich w DB
                                formState!!.removeControlStatesWithPrefix("$controlName[${rowToRemove.id}]")
                            }

                            // Usuń wiersz z listy
                            currentRows.removeAt(index)

                            // Zaktualizuj tylko indeksy dla wyświetlania (UUID pozostaje bez zmian)
                            currentRows.forEachIndexed { newIndex, row ->
                                currentRows[newIndex] = row.copy(index = newIndex)
                            }

                            controlState.value.value = currentRows
                            updateState(controlState)
                        }
                    } else null,
                    content = {
                        RepeatableRowContent(
                            row = row
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Komunikat o błędzie
            RenderError(controlState)
        }
    }

    @Composable
    private fun RepeatableHeader(
        label: String?,
        onAddClick: () -> Unit,
        canAdd: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (canAdd) {
                FilledTonalButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dodaj")
                }
            }
        }
    }

    @Composable
    private fun RepeatableRowCard(
        row: RepeatableRow,
        index: Int,
        onDelete: (() -> Unit)?,
        content: @Composable () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            var isExpanded by remember { mutableStateOf(true) }

            Column {
                // Nagłówek karty
                RepeatableRowHeader(
                    index = index,
                    isExpanded = isExpanded,
                    onExpandToggle = { isExpanded = !isExpanded },
                    onDelete = onDelete
                )

                // Zawartość karty
                AnimatedVisibility(visible = isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }

    @Composable
    private fun RepeatableRowHeader(
        index: Int,
        isExpanded: Boolean,
        onExpandToggle: () -> Unit,
        onDelete: (() -> Unit)?
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            onClick = onExpandToggle
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Element ${index + 1}",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ikona rozwijania
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Przycisk usuwania
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Usuń",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RepeatableRowContent(
        row: RepeatableRow
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rowOrder.forEach { fieldName ->
                rowControls[fieldName]?.let { control ->
                    val hierarchicalName = "$controlName[${row.id}].$fieldName"
                    // Pobierz stan bezpośrednio z FormState zamiast z przekazanej mapy
                    val state = this@RepeatableControl.formState!!.getControlState(hierarchicalName)
                    if (state != null) {
                        // Używamy globalnego kontekstu - states jest teraz reactive
                        control.Render(
                            controlName = hierarchicalName,
                            controlState = state
                        )

                        // Odstęp między kontrolkami
                        if (fieldName != rowOrder.last()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    // YYYY wiem co robię? W każdym razie funkcja nie jest wywoływana z nazwanymi parametrami
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun convertToResult(
        state: ControlState<*>, // Ten state to ControlState<List<RepeatableRow>> dla RepeatableControl
        outerControls: Map<String, Control<*>>,
        outerStates: Map<String, ControlState<*>>
    ): Any? {

        @Suppress("UNCHECKED_CAST")
        val controlState = state as ControlState<List<RepeatableRow>>

        requireNotNull(controlName) { "controlName nie został ustawiony dla RepeatableControl" }

        val (newRows, deletedRows, changedRows) = getRowTypes(
            controlState,
            controlName!!,
            rowControls,
            outerStates
        )

        val deletedRowsValues = deletedRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldControlState = outerStates[hierarchicalName]!!
                val value = control.getResult(hierarchicalName, fieldControlState, outerControls, outerStates)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val newRowsValues = newRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldControlState = outerStates[hierarchicalName]!!
                val value = control.getResult(hierarchicalName, fieldControlState, outerControls, outerStates)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val changedRowsValues = changedRows.map { row ->
            rowControls.mapValues { (fieldName, control) ->
                val hierarchicalName = "$controlName[${row.id}].$fieldName"
                val fieldControlState = outerStates[hierarchicalName]!!
                val value = control.getResult(hierarchicalName, fieldControlState, outerControls, outerStates)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        // Wyczyść stany usuniętych wierszy które były oryginalne
        deletedRows.forEach { row ->
            formState!!.removeControlStatesWithPrefix("$controlName[${row.id}]")
        }

        return RepeatableResultValue(
            deletedRows = deletedRowsValues,
            addedRows = newRowsValues,
            modifiedRows = changedRowsValues
        )
    }
}