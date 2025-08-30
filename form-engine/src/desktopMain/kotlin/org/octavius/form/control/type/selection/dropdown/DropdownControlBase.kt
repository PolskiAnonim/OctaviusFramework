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
import org.octavius.data.contract.ColumnInfo
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.layout.RenderNormalLabel
import org.octavius.localization.T
import org.octavius.ui.theme.FormSpacing

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
    required: Boolean?,
    dependencies: Map<String, ControlDependency<*>>?,
    actions: List<ControlAction<T>>?
) : Control<T>(
    label,
    columnInfo,
    required,
    dependencies,
    hasStandardLayout = false,
    actions = actions
) {

    // Flagi konfiguracyjne dla kontrolki
    protected open val supportSearch: Boolean = false
    protected open val supportPagination: Boolean = false

    // Metody abstrakcyjne, które muszą być zaimplementowane w podklasach
    protected abstract fun getDisplayText(value: T?): String?
    protected abstract fun loadOptions(searchQuery: String, page: Long): Pair<List<DropdownOption<T>>, Long>

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Display(controlName: String, controlState: ControlState<T>, isRequired: Boolean) {
        var expanded by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var options by remember { mutableStateOf<List<DropdownOption<T>>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var currentPage by remember { mutableStateOf(0L) }
        var totalPages by remember { mutableStateOf(1L) }
        val scope = rememberCoroutineScope()

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

        Column(modifier = Modifier.fillMaxWidth()) {
            RenderNormalLabel(label, isRequired)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pole z wybraną wartością
                OutlinedTextField(
                    value = getDisplayText(controlState.value.value)
                        ?: (if (!isRequired) T.get("form.dropdown.noSelection") else T.get("form.dropdown.selectOption")),
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
                    // Pole wyszukiwania jeśli obsługiwane
                    if (supportSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                currentPage = 0  // Reset page on search
                            },
                            placeholder = { Text(T.get("search.placeholder")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = FormSpacing.dropdownPaddingHorizontal,
                                    vertical = FormSpacing.dropdownPaddingVertical
                                ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = T.get("search.search")
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        currentPage = 0
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = T.get("search.clear")
                                        )
                                    }
                                }
                            }
                        )
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FormSpacing.sectionPadding),
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
                                text = { Text(T.get("form.dropdown.noSelection")) },
                                onClick = {
                                    controlState.value.value = null
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
                                        T.get("form.dropdown.noResults"),
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
                                        executeActions(controlName, option.value, scope)
                                        expanded = false
                                    }
                                )
                            }

                            // Panel paginacji, tylko jeśli jest więcej niż jedna strona
                            if (supportPagination && totalPages > 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = FormSpacing.itemSpacing))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = FormSpacing.dropdownPaddingHorizontal,
                                            vertical = FormSpacing.dropdownPaddingVertical
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (currentPage > 0) currentPage-- },
                                        enabled = currentPage > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = T.get("pagination.previousPage")
                                        )
                                    }

                                    Text(T.get("pagination.page") + " ${currentPage + 1} " + T.get("pagination.of") + " $totalPages")

                                    IconButton(
                                        onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                        enabled = currentPage < totalPages - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = T.get("pagination.nextPage")
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