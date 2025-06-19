package org.octavius.form.control.type.selection.dropdown

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
import org.octavius.form.ColumnInfo
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.layout.RenderNormalLabel
import org.octavius.form.control.validator.DefaultValidator

/**
 * Bazowa klasa abstrakcyjna dla kontrolek listy rozwijanej (dropdown).
 *
 * Zapewnia wspólną funkcjonalność dla wszystkich kontrolek typu dropdown, w tym:
 * - Obsługę ładowania opcji z różnych źródeł danych
 * - Opcjonalne wyszukiwanie w opcjach
 * - Opcjonalną paginację wyników
 * - Standardowy interfejs użytkownika z Material Design 3
 * - Obsługę wartości null dla kontrolek niewymaganych
 *
 * Podklasy muszą zaimplementować metody abstrakcyjne:
 * - [getDisplayText] - formatowanie wyświetlanego tekstu dla wybranej wartości
 * - [loadOptions] - ładowanie opcji z uwzględnieniem wyszukiwania i paginacji
 *
 * @param T typ wartości przechowywanych w opcjach dropdown
 */
abstract class DropdownControlBase<T : Any>(
    label: String?,
    columnInfo: ColumnInfo?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<T>(
    label,
    columnInfo,
    required,
    dependencies,
    hasStandardLayout = false
) {
    override val validator: ControlValidator<T> = DefaultValidator()

    // Flagi konfiguracyjne dla kontrolki
    protected open val supportSearch: Boolean = false
    protected open val supportPagination: Boolean = false

    // Metody abstrakcyjne, które muszą być zaimplementowane w podklasach
    protected abstract fun getDisplayText(value: T?): String?
    protected abstract fun loadOptions(searchQuery: String, page: Int): Pair<List<DropdownOption<T>>, Int>

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Display(controlName: String, controlState: ControlState<T>, isRequired: Boolean) {
        var expanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var options by remember { mutableStateOf<List<DropdownOption<T>>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var currentPage by remember { mutableStateOf(1) }
        var totalPages by remember { mutableStateOf(1) }

        // Efekt pobierający opcje gdy menu jest otwarte
        LaunchedEffect(expanded, searchQuery, currentPage) {
            if (expanded) {
                isLoading = true
                try {
                    val (items, pages) = loadOptions(searchQuery, currentPage)
                    options = items
                    totalPages = pages
                } finally {
                    isLoading = false
                }
            }
        }

        Column(modifier = Modifier.Companion.fillMaxWidth()) {
            RenderNormalLabel(label, isRequired)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.Companion.fillMaxWidth()
            ) {
                // Pole z wybraną wartością
                OutlinedTextField(
                    value = getDisplayText(controlState.value.value)
                        ?: (if (!isRequired) "Brak wyboru" else "Wybierz opcję"),
                    onValueChange = { },  // Nie pozwalamy na edycję ręczną
                    readOnly = true,      // Pole tylko do odczytu
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.Companion.PrimaryNotEditable)
                )

                // Menu z opcjami
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Pole wyszukiwania jeśli obsługiwane
                    if (supportSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                currentPage = 1  // Reset page on search
                            },
                            placeholder = { Text("Szukaj...") },
                            singleLine = true,
                            modifier = Modifier.Companion
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
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Companion.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.Companion.size(24.dp)
                            )
                        }
                    } else {
                        // Opcja null tylko jeśli kontrolka nie jest wymagana
                        if (!isRequired) {
                            DropdownMenuItem(
                                text = { Text("Brak wyboru") },
                                onClick = {
                                    controlState.value.value = null
                                    updateState(controlState)
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
                                        controlState.value.value = option.value
                                        updateState(controlState)
                                        expanded = false
                                    }
                                )
                            }

                            // Panel paginacji, tylko jeśli jest więcej niż jedna strona
                            if (supportPagination && totalPages > 1) {
                                HorizontalDivider(modifier = Modifier.Companion.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.Companion
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Companion.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (currentPage > 1) currentPage-- },
                                        enabled = currentPage > 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Poprzednia strona"
                                        )
                                    }

                                    Text("Strona $currentPage z $totalPages")

                                    IconButton(
                                        onClick = { if (currentPage < totalPages) currentPage++ },
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
            DisplayFieldErrors(controlName)
        }
    }
}