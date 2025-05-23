package org.octavius.novels.form.control.type

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

data class RepeatableRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    val controls: Map<String, Control<*>>,
    val states: MutableMap<String, ControlState<*>> = mutableMapOf(),
    var isNew: Boolean = true
)

class RepeatableControl(
    val rowControlsFactory: () -> Map<String, Control<*>>,
    val rowOrder: List<String>,
    val uniqueFields: List<String> = emptyList(),
    val minRows: Int = 0,
    val maxRows: Int? = null,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<List<Map<String, Any?>>>(label, null, hidden, required, dependencies) {

    override val validator: ControlValidator<List<Map<String, Any?>>> = RepeatableValidator(uniqueFields)

    // Stan wierszy
    private val rows = mutableStateListOf<RepeatableRow>()

    override fun setInitValue(value: Any?): ControlState<List<Map<String, Any?>>> {
        val state = ControlState<List<Map<String, Any?>>>()

        @Suppress("UNCHECKED_CAST")
        val initialRows = value as? List<Map<String, Any?>> ?: emptyList()

        rows.clear()
        initialRows.forEach { rowData ->
            val row = createRow()
            row.isNew = false

            // Ustaw wartości dla kontrolek w wierszu
            rowData.forEach { (fieldName, fieldValue) ->
                val control = row.controls[fieldName]
                if (control != null) {
                    row.states[fieldName] = control.setInitValue(fieldValue)
                }
            }

            rows.add(row)
        }

        // Dodaj minimalne wiersze jeśli potrzeba
        while (rows.size < minRows) {
            rows.add(createRow())
        }

        state.value.value = getRowsData()
        return state
    }

    private fun createRow(): RepeatableRow {
        val controls = rowControlsFactory()
        val states = mutableMapOf<String, ControlState<*>>()

        controls.forEach { (name, control) ->
            states[name] = control.setInitValue(null)
        }

        return RepeatableRow(controls = controls, states = states)
    }

    private fun getRowsData(): List<Map<String, Any?>> {
        return rows.map { row ->
            row.controls.mapNotNull { (name, control) ->
                val state = row.states[name]
                val value = state?.value?.value
                if (value != null) name to value else null
            }.toMap()
        }
    }

    @Composable
    override fun Display(
        controlState: ControlState<List<Map<String, Any?>>>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        var expandedStates by remember { mutableStateOf(rows.map { true }.toMutableList()) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Nagłówek sekcji
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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

                // Przycisk dodawania nowego wiersza
                if (maxRows == null || rows.size < maxRows) {
                    FilledTonalButton(
                        onClick = {
                            rows.add(createRow())
                            expandedStates.add(true)
                            controlState?.value?.value = getRowsData()
                            controlState?.dirty?.value = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Dodaj"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dodaj")
                    }
                }
            }

            // Lista wierszy
            rows.forEachIndexed { index, row ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column {
                        // Nagłówek wiersza
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Element ${index + 1}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Row {
                                    // Przycisk zwijania/rozwijania
                                    IconButton(
                                        onClick = {
                                            expandedStates = expandedStates.toMutableList().apply {
                                                this[index] = !this[index]
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (expandedStates.getOrNull(index) == true)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expandedStates.getOrNull(index) == true) "Zwiń" else "Rozwiń"
                                        )
                                    }

                                    // Przycisk usuwania
                                    if (rows.size > minRows) {
                                        IconButton(
                                            onClick = {
                                                rows.removeAt(index)
                                                expandedStates.removeAt(index)
                                                controlState?.value?.value = getRowsData()
                                                controlState?.dirty?.value = true
                                            }
                                        ) {
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

                        // Zawartość wiersza
                        AnimatedVisibility(visible = expandedStates.getOrNull(index) == true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                rowOrder.forEach { controlName ->
                                    row.controls[controlName]?.let { control ->
                                        // Renderuj kontrolkę z kontekstem wiersza
                                        control.Render(
                                            controlState = row.states[controlName],
                                            controls = row.controls,
                                            states = row.states
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Komunikat o błędzie dla całej kontrolki
            controlState?.error?.value?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }
    }

    override fun convertToResult(value: Any?): Any? {
        return rows.map { row ->
            val rowData = mutableMapOf<String, Any?>()

            row.controls.forEach { (name, control) ->
                val state = row.states[name]
                val controlValue = state?.value?.value
                if (controlValue != null) {
                    rowData[name] = control.getResult(controlValue, row.controls, row.states)
                }
            }

            // Dodaj informację czy wiersz jest nowy
            rowData["_isNew"] = row.isNew
            rowData
        }
    }
}

// Walidator dla RepeatableControl
class RepeatableValidator(
    private val uniqueFields: List<String>
) : ControlValidator<List<Map<String, Any?>>>() {

    override fun validateSpecific(state: ControlState<*>) {
        @Suppress("UNCHECKED_CAST")
        val rows = state.value.value as? List<Map<String, Any?>> ?: return

        // Sprawdź unikalność
        if (uniqueFields.isNotEmpty()) {
            val seenValues = mutableSetOf<List<Any?>>()

            for ((index, row) in rows.withIndex()) {
                val uniqueKey = uniqueFields.map { field -> row[field] }

                if (uniqueKey.any { it != null }) { // Ignoruj puste wiersze
                    if (!seenValues.add(uniqueKey)) {
                        state.error.value = "Duplikat wartości w wierszu ${index + 1}"
                        return
                    }
                }
            }
        }
    }
}