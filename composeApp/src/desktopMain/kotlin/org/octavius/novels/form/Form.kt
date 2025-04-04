package org.octavius.novels.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlState
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.util.Converters.camelToSnakeCase

abstract class Form {
    // Schema and state
    private val formSchema: FormControls by lazy {
        createSchema()
    }

    protected abstract fun createSchema(): FormControls

    private val formState: MutableMap<String, ControlState<*>> = mutableMapOf()

    protected abstract fun defineTableRelations(): List<TableRelation>

    // Loading data

    private var loadedId: Int? = null

    fun loadData(id: Int) {

        val tableRelations = defineTableRelations()
        val data = DatabaseManager.getEntityWithRelations(id, tableRelations)

        for ((controlName, control) in formSchema.controls) {
            if (control.fieldName != null && control.tableName != null) {
                val columnName = camelToSnakeCase(control.fieldName)
                val tableName = camelToSnakeCase(control.tableName)
                val value = data[ColumnInfo(tableName, columnName)]
                formState[controlName] = control.setInitValue(value)
            }
        }
        loadedId = id
    }

    // Metoda czyszcząca formularz
    fun clearForm() {
        for ((controlName, control) in formSchema.controls) {
            if (control.fieldName != null && control.tableName != null) {
                formState[controlName] = control.setInitValue(null)
            }
        }
    }

    // Snackbar

    private val snackbarHostState = SnackbarHostState()
    private val showSnackbar = mutableStateOf(false)
    private val snackbarMessage = mutableStateOf("")

    // Display

    @Composable
    fun display() {
        val navigator = LocalNavigator.current
        val scrollState = rememberScrollState()

        val scope = rememberCoroutineScope()

        LaunchedEffect(showSnackbar.value) {
            if (showSnackbar.value) {
                scope.launch {
                    snackbarHostState.showSnackbar(message = snackbarMessage.value)
                    showSnackbar.value = false
                }
            }
        }

        Scaffold(
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { navigator.removeScreen() }
                        ) {
                            Text("Anuluj")
                        }

                        Button(
                            onClick = {
                                try {
                                    val success = saveForm()
                                    if (success) {
                                        snackbarMessage.value = "Formularz został zapisany pomyślnie"
                                        showSnackbar.value = true
                                        // Wróć do poprzedniego ekranu po zapisie
                                        navigator.removeScreen()
                                    } else {
                                        snackbarMessage.value  = "Formularz zawiera błędy"
                                        showSnackbar.value = true
                                    }
                                } catch (e: Exception) {
                                    snackbarMessage.value = "Wystąpił błąd: ${e.message}"
                                    showSnackbar.value = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Zapisz",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Zapisz")
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState) // Dodajemy przewijanie pionowe
            ) {
                formSchema.order.forEach {
                    formSchema.controls[it]?.render(it, formSchema.controls, formState)
                }

                // Dodajemy dodatkowy odstęp na dole, aby zapewnić, że dolne elementy
                // nie będą zasłonięte przez dolny pasek
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Validate

    // Walidacja formularza
    private fun validateForm(): Boolean {
        var isValid = true

        for ((controlName, control) in formSchema.controls) {
            // Pobierz stan kontrolki
            val state = formState[controlName] ?: continue
            control.validateControl(state, formSchema.controls, formState)

            if (state.error.value != null) {
                isValid = false
            }
        }

        return isValid
    }

    // Save

    // Metoda zbierająca dane z formularza do zapisu
    private fun collectFormData(): Map<String, Map<String, Any?>> {
        val result = mutableMapOf<String, MutableMap<String, Any?>>()

        for ((controlName, control) in formSchema.controls) {
            var value = formState[controlName]?.value?.value ?: continue
            value = control.getResult(value, formSchema.controls, formState) ?: continue

            // Pobierz tablę i pole
            val tableName = control.tableName ?: continue
            val fieldName = control.fieldName ?: continue

            // Dodaj do odpowiedniej tabeli
            if (!result.containsKey(tableName)) {
                result[tableName] = mutableMapOf()
            }

            // Dodaj wartość
            result[tableName]!![camelToSnakeCase(fieldName)] = value
        }

        // Dodaj ID do głównej tabeli, jeśli istnieje
        if (loadedId != null) {
            val mainTable = defineTableRelations().firstOrNull()?.tableName
            if (mainTable != null && result.containsKey(mainTable)) {
                result[mainTable]!!["id"] = loadedId
            }
        }

        return result
    }

    // Metoda do zapisu formularza
    fun saveForm(): Boolean {
        // Waliduj formularz
        if (!validateForm()) {
            return false
        }

        // Zbierz dane
        val formData = collectFormData()

        // Przygotuj dane do zapisu
        val tableRelations = defineTableRelations()
        val mainTable = tableRelations.firstOrNull()?.tableName ?: return false

        val mainData = formData[mainTable] ?: mutableMapOf()
        val relatedData = mutableMapOf<String, Pair<String, Map<String, Any?>>>()

        // Przygotuj dane dla powiązanych tabel
        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            val tableName = relation.tableName
            val joinCondition = relation.joinCondition

            val tableData = formData[tableName]
            if (tableData != null) {
                relatedData[tableName] = Pair(joinCondition, tableData)
            }
        }

        try {
            // Zapisz do bazy danych
            val savedId = DatabaseManager.saveOrUpdateEntity(mainTable, mainData, relatedData)

            if (savedId != null) {
                // Aktualizuj ID w formularzu
                loadedId = savedId
                return true
            }

            return false
        } catch (e: Exception) {
            println("Błąd zapisu formularza: ${e.message}")
            return false
        }
    }

}