package org.octavius.novels.form.control.type

import androidx.compose.runtime.*
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.type.repeatable.RepeatableRow
import org.octavius.novels.form.control.type.repeatable.RepeatableResultValue
import org.octavius.novels.form.control.type.repeatable.createRow
import org.octavius.novels.form.control.type.repeatable.getRowTypes
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.RepeatableValidator


class RepeatableControl(
    val rowControls: Map<String, Control<*>>,
    val rowOrder: List<String>,
    val uniqueFields: List<String> = emptyList(),
    val minRows: Int = 0,
    val maxRows: Int? = null,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<List<RepeatableRow>>(label, null, hidden, required, dependencies) {

    override val validator: ControlValidator<List<RepeatableRow>> = RepeatableValidator(uniqueFields)

    override fun copyInitToValue(init: List<RepeatableRow>): List<RepeatableRow> {
        return init.map { RepeatableRow(
            id = it.id,
            states = it.states.mapValues { (_, controlState) ->
                ControlState<Any>().apply {
                    value.value = controlState.value.value
                    initValue.value = controlState.initValue.value
                    error.value = controlState.error.value
                    dirty.value = controlState.dirty.value
                }
            }.toMutableMap()
        ) }
    }

    override fun setInitValue(value: Any?): ControlState<List<RepeatableRow>> {
        @Suppress("UNCHECKED_CAST")
        val initialRows = value as? List<Map<String, Any?>> ?: emptyList()

        // State
        val initialRowsList = initialRows.map { initialRow ->
            RepeatableRow(
                states = initialRow.mapValues { (key, value) ->
                    rowControls[key]!!.setInitValue(value)
                }.toMutableMap()
            )
        }

        val additionalRows = mutableListOf<RepeatableRow>()

        // Dodaj minimalne wiersze jeśli potrzeba
        while (initialRowsList.size + additionalRows.size < minRows) {
            additionalRows.add(createRow(rowControls))
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
        controlState: ControlState<List<RepeatableRow>>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        // TODO: DO NAPRAWY/ZROBIENIA
//        var expandedStates by remember { mutableStateOf(rows.map { true }.toMutableList()) }
//
//        Column(modifier = Modifier.fillMaxWidth()) {
//            // Nagłówek sekcji
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 8.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                label?.let {
//                    Text(
//                        text = it,
//                        style = MaterialTheme.typography.titleLarge,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                }
//
//                // Przycisk dodawania nowego wiersza
//                if (maxRows == null || rows.size < maxRows) {
//                    FilledTonalButton(
//                        onClick = {
//                            rows.add(createRow())
//                            expandedStates.add(true)
//                            controlState?.value?.value = getRowsData()
//                            controlState?.dirty?.value = true
//                        }
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Add,
//                            contentDescription = "Dodaj"
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Dodaj")
//                    }
//                }
//            }
//
//            // Lista wierszy
//            rows.forEachIndexed { index, row ->
//                ElevatedCard(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 4.dp)
//                ) {
//                    Column {
//                        // Nagłówek wiersza
//                        Surface(
//                            modifier = Modifier.fillMaxWidth(),
//                            color = MaterialTheme.colorScheme.surfaceVariant
//                        ) {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 16.dp, vertical = 8.dp),
//                                horizontalArrangement = Arrangement.SpaceBetween,
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text(
//                                    text = "Element ${index + 1}",
//                                    style = MaterialTheme.typography.titleMedium
//                                )
//
//                                Row {
//                                    // Przycisk zwijania/rozwijania
//                                    IconButton(
//                                        onClick = {
//                                            expandedStates = expandedStates.toMutableList().apply {
//                                                this[index] = !this[index]
//                                            }
//                                        }
//                                    ) {
//                                        Icon(
//                                            imageVector = if (expandedStates.getOrNull(index) == true)
//                                                Icons.Default.KeyboardArrowUp
//                                            else
//                                                Icons.Default.KeyboardArrowDown,
//                                            contentDescription = if (expandedStates.getOrNull(index) == true) "Zwiń" else "Rozwiń"
//                                        )
//                                    }
//
//                                    // Przycisk usuwania
//                                    if (rows.size > minRows) {
//                                        IconButton(
//                                            onClick = {
//                                                rows.removeAt(index)
//                                                expandedStates.removeAt(index)
//                                                controlState?.value?.value = getRowsData()
//                                                controlState?.dirty?.value = true
//                                            }
//                                        ) {
//                                            Icon(
//                                                imageVector = Icons.Default.Delete,
//                                                contentDescription = "Usuń",
//                                                tint = MaterialTheme.colorScheme.error
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//
//                        // Zawartość wiersza
//                        AnimatedVisibility(visible = expandedStates.getOrNull(index) == true) {
//                            Column(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(16.dp)
//                            ) {
//                                rowOrder.forEach { controlName ->
//                                    row.controls[controlName]?.let { control ->
//                                        // Renderuj kontrolkę z kontekstem wiersza
//                                        control.Render(
//                                            controlState = row.states[controlName],
//                                            controls = row.controls,
//                                            states = row.states
//                                        )
//                                        Spacer(modifier = Modifier.height(8.dp))
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // Komunikat o błędzie dla całej kontrolki
//            controlState?.error?.value?.let { error ->
//                Text(
//                    text = error,
//                    color = MaterialTheme.colorScheme.error,
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
//                )
//            }
//        }
    }

//    override fun convertToResult(state: ControlState<*>, outerControls: Map<String, Control<*>>, outerStates: Map<String, ControlState<*>>): Any? {

//        // Kontrolka zawsze ma co najmniej pustą listę jako wartość
//        // Nowe wiersze
//        val newRows = controlState.value.value!!.filter { row -> row.id !in controlState.initValue.value!!.map { it.id } }
//        // Usunięte wiersze
//        val deletedRows = controlState.initValue.value!!.filter { row -> row.id !in controlState.value.value!!.map { it.id } }
//        // Zmienione wiersze
//        val changedRows = controlState.value.value!!.filter { row -> !row.states.values.none { it.dirty.value } }
//
//        val newRowsValues = newRows.map { row -> row.states.mapValues { (name, controlState) -> {
//            val result = rowControls[name]!!.getResult(controlState, outerControls, outerStates)
//            name to result
//        }
//        }.toMap() }
//
//    }

    // YYYY wiem co robię? W każdym razie funkcja nie jest wywoływana z nazwanymi parametrami
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun convertToResult(
        state: ControlState<*>, // Ten state to ControlState<List<RepeatableRow>> dla RepeatableControl
        outerControls: Map<String, Control<*>>,
        outerStates: Map<String, ControlState<*>>,
        useInitValue: Boolean // Nie ma jak zmienna którą sama kontrolka sobię zgotowała a jest w niej useless
    ): Any? {

        @Suppress("UNCHECKED_CAST")
        val controlState = state as ControlState<List<RepeatableRow>>

        val (newRows, deletedRows, changedRows) = getRowTypes(controlState)

        val deletedRowsValues = deletedRows.map { row ->
                row.states.mapValues { (name, fieldControlState) ->
                    // Dla usuniętych wierszy, `fieldControlState` pochodzi z wiersza, który był w `initialListOfRows`.
                    // Chcemy wynik oparty na `fieldControlState.initValue.value`.
                    // Przekazujemy `useInitValue = true`.
                    val value = rowControls[name]!!.getResult(fieldControlState, outerControls, outerStates, useInitValue = true)
                    name to ControlResultData(value, fieldControlState.dirty.value)
                }
        }

        val newRowsValues = newRows.map { row ->
            row.states.mapValues { (name, fieldControlState) ->
                val value = rowControls[name]!!.getResult(fieldControlState, outerControls, outerStates)
                name to ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val changedRowsValues = changedRows.map { row ->
            row.states.mapValues { (name, fieldControlState) ->
                val value = rowControls[name]!!.getResult(fieldControlState, outerControls, outerStates)
                name to ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        return RepeatableResultValue(
            deletedRows = deletedRowsValues,
            addedRows = newRowsValues,
            modifiedRows = changedRowsValues
        )
    }
}