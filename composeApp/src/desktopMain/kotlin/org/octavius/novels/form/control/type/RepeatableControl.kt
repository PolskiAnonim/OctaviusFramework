package org.octavius.novels.form.control.type

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.RenderError
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
                }.toMap()
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
        controlState: ControlState<List<RepeatableRow>>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {
            // Nagłówek z przyciskiem dodawania
            RepeatableHeader(
                label = label,
                onAddClick = {
                    val newRow = createRow(rowControls)
                    val currentRows = controlState.value.value!!.toMutableList()
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
                            currentRows.removeAt(index)
                            controlState.value.value = currentRows
                            updateState(controlState)
                        }
                    } else null,
                    content = {
                        RepeatableRowContent(
                            row = row,
                            rowOrder = rowOrder,
                            rowControls = rowControls
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
        row: RepeatableRow,
        rowOrder: List<String>,
        rowControls: Map<String, Control<*>>
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rowOrder.forEach { controlName ->
                rowControls[controlName]?.let { control ->
                    val state = row.states[controlName]
                    if (state != null) {
                        // Tworzymy lokalny kontekst kontrolek dla wiersza
                        control.Render(
                            controlState = state,
                            controls = rowControls,
                            states = row.states
                        )

                        // Odstęp między kontrolkami
                        if (controlName != rowOrder.last()) {
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

        val (newRows, deletedRows, changedRows) = getRowTypes(controlState)

        val deletedRowsValues = deletedRows.map { row ->
            row.states.mapValues { (name, fieldControlState) ->
                // Dla usuniętych wierszy, `fieldControlState` pochodzi z wiersza, który był w `initialListOfRows`.
                // Teoretycznie wynik powinien być na podstawie initValue no ale i tak potrzebne są zwykle tylko ukryte
                // kontrolki których nie da się zmienić
                val value = rowControls[name]!!.getResult(fieldControlState, outerControls, outerStates)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val newRowsValues = newRows.map { row ->
            row.states.mapValues { (name, fieldControlState) ->
                val value = rowControls[name]!!.getResult(fieldControlState, outerControls, outerStates)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        val changedRowsValues = changedRows.map { row ->
            row.states.mapValues { (name, fieldControlState) ->
                val value = rowControls[name]!!.getResult(fieldControlState, outerControls, outerStates)
                ControlResultData(value, fieldControlState.dirty.value)
            }
        }

        return RepeatableResultValue(
            deletedRows = deletedRowsValues,
            addedRows = newRowsValues,
            modifiedRows = changedRowsValues
        )
    }
}