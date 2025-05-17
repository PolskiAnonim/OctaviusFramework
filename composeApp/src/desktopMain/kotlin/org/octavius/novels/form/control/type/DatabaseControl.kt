package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

data class DatabaseOption(
    val id: Int,
    val displayText: String
)

class DatabaseControl(
    columnInfo: ColumnInfo?,
    label: String?,
    private val relatedTable: String,
    private val displayColumn: String,
    private val pageSize: Int = 10,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Int>(
    label,
    columnInfo,
    hidden,
    required,
    dependencies
) {
    override val validator: ControlValidator<Int> = DefaultValidator()

    @OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
    @Composable
    override fun Display(
        controlState: ControlState<Int>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ) {
        controlState!!.let { ctrlState ->
            var expanded by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }
            var options by remember { mutableStateOf<List<DatabaseOption>>(emptyList()) }
            var selectedOption by remember { mutableStateOf<DatabaseOption?>(null) }
            var isLoading by remember { mutableStateOf(false) }
            var currentPage by remember { mutableStateOf(1) }
            var totalPages by remember { mutableStateOf(1) }

            // Sprawdzamy, czy kontrolka jest wymagana
            val isRequired = validator.isControlRequired(this, controls, states)

            // Efekt pobierający wybraną opcję przy inicjalizacji
            LaunchedEffect(ctrlState.value.value) {
                if (ctrlState.value.value != null) {
                    val item = loadSelectedItem(ctrlState.value.value!!)
                    selectedOption = item
                } else {
                    selectedOption = null
                }
            }

            // Obsługa wyszukiwania z debounce
            val searchQueryFlow = remember { MutableStateFlow("") }

            LaunchedEffect(searchQueryFlow, currentPage) {
                searchQueryFlow
                    .debounce(300)
                    .collectLatest { query ->
                        isLoading = true
                        try {
                            val (items, pages) = searchItems(query, currentPage, pageSize)
                            options = items
                            totalPages = pages
                        } finally {
                            isLoading = false
                        }
                    }
            }

            // Aktualizacja flow wyszukiwania
            LaunchedEffect(searchQuery) {
                currentPage = 1  // Resetuj stronę przy nowym wyszukiwaniu
                searchQueryFlow.value = searchQuery
            }

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                // Label z gwiazdką jeśli pole jest wymagane
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = label ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (isRequired) {
                        Text(
                            text = " *",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = it
                        if (it && options.isEmpty()) {
                            // Przy otwarciu menu, jeśli jeszcze nie mamy opcji, pobieramy je
                            searchQueryFlow.value = searchQuery
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pole z wybraną wartością
                    OutlinedTextField(
                        value = selectedOption?.displayText ?: (if (!isRequired) "Brak wyboru" else "Wybierz opcję"),
                        onValueChange = { },  // Nie pozwalamy na edycję ręczną
                        readOnly = true,      // Pole tylko do odczytu
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    // Menu z opcjami
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Pole wyszukiwania
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Szukaj...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Szukaj"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        // Reset page when clearing search
                                        currentPage = 1
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Wyczyść"
                                        )
                                    }
                                }
                            }
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            // Opcja null tylko jeśli kontrolka nie jest wymagana
                            if (!isRequired) {
                                DropdownMenuItem(
                                    text = { Text("Brak wyboru") },
                                    onClick = {
                                        selectedOption = null
                                        ctrlState.value.value = null
                                        updateState(ctrlState)
                                        expanded = false
                                    }
                                )

                                HorizontalDivider()
                            }

                            if (options.isEmpty() && !isLoading) {
                                DropdownMenuItem(
                                    enabled = false,
                                    text = {
                                        Text(
                                            "Brak wyników",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    },
                                    onClick = {}
                                )
                            } else {
                                // Lista opcji
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayText) },
                                        onClick = {
                                            selectedOption = option
                                            ctrlState.value.value = option.id
                                            updateState(ctrlState)
                                            expanded = false
                                        }
                                    )
                                }

                                // Panel paginacji, tylko jeśli jest więcej niż jedna strona
                                if (totalPages > 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (currentPage > 1) {
                                                    currentPage--
                                                    searchQueryFlow.value = searchQuery
                                                }
                                            },
                                            enabled = currentPage > 1
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Poprzednia strona"
                                            )
                                        }

                                        Text("Strona $currentPage z $totalPages")

                                        IconButton(
                                            onClick = {
                                                if (currentPage < totalPages) {
                                                    currentPage++
                                                    searchQueryFlow.value = searchQuery
                                                }
                                            },
                                            enabled = currentPage < totalPages
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Następna strona"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Komunikat o błędzie
                if (ctrlState.error.value != null) {
                    Text(
                        text = ctrlState.error.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }

    // Metoda do wyszukiwania elementów z paginacją
    private fun searchItems(query: String, page: Int, pageSize: Int): Pair<List<DatabaseOption>, Int> {
        val sql = if (query.isEmpty()) {
            "SELECT id, $displayColumn FROM $relatedTable ORDER BY $displayColumn"
        } else {
            "SELECT id, $displayColumn FROM $relatedTable WHERE $displayColumn ILIKE ? ORDER BY $displayColumn"
        }

        val params = if (query.isEmpty()) emptyList() else listOf("%$query%")

        return try {
            val (results, totalPages) = DatabaseManager.executeQuery(sql, params, page, pageSize)
            val mappedResults = results.groupBy { it[ColumnInfo(relatedTable, "id")] }
                .map { (id, items) ->
                    val displayValue = items.firstOrNull()?.get(ColumnInfo(relatedTable, displayColumn)) as? String ?: ""
                    DatabaseOption(id as Int, displayValue)
                }
            Pair(mappedResults, totalPages.toInt())
        } catch (e: Exception) {
            println("Błąd podczas wyszukiwania elementów: ${e.message}")
            Pair(emptyList(), 1)
        }
    }

    // Metoda do ładowania wybranego elementu
    private fun loadSelectedItem(id: Int): DatabaseOption? {
        val sql = "SELECT id, $displayColumn FROM $relatedTable WHERE id = ?"

        return try {
            val results = DatabaseManager.executeQuery(sql, listOf(id)).first
            if (results.isNotEmpty()) {
                val item = results.first()
                val displayValue = item[ColumnInfo(relatedTable, displayColumn)] as? String ?: ""
                DatabaseOption(id, displayValue)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Błąd podczas ładowania elementu: ${e.message}")
            null
        }
    }
}