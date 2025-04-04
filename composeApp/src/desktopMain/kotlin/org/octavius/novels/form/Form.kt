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
import org.octavius.novels.form.control.ControlState
import org.octavius.novels.navigator.LocalNavigator

abstract class Form {
    // Schema and state
    private val formSchema: FormControls by lazy {
        createSchema()
    }

    protected abstract fun createSchema(): FormControls

    private val formState: MutableMap<String, ControlState<*>> = mutableMapOf()

    protected abstract fun defineTableRelations(): List<TableRelation>

    // Loading data

    protected var loadedId: Int? = null

    fun loadData(id: Int) {

        val tableRelations = defineTableRelations()
        val data = DatabaseManager.getEntityWithRelations(id, tableRelations)

        for ((controlName, control) in formSchema.controls) {
            if (control.fieldName != null && control.tableName != null) {
                val value = data[ColumnInfo(control.tableName, control.fieldName)]
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
    private fun collectFormData(): Map<String, Map<String, ControlResultData>> {
        val result = mutableMapOf<String, MutableMap<String, Any?>>()

        for ((controlName, control) in formSchema.controls) {
            // Pobierz tablę i pole
            val tableName = control.tableName ?: continue
            val fieldName = control.fieldName ?: continue

            val state = formState[controlName] ?: continue
            var value = state.value.value
            value = control.getResult(value, formSchema.controls, formState)

            // Dodaj do odpowiedniej tabeli
            if (!result.containsKey(tableName)) {
                result[tableName] = mutableMapOf()
            }

            // Dodaj wartość
            result[tableName]!![fieldName] = ControlResultData(value, state.dirty.value)
        }

        @Suppress("UNCHECKED_CAST")
        return result as Map<String, Map<String, ControlResultData>>
    }

    // Metoda do przetwarzania danych przed zapisem
    protected abstract fun processFormData(formData: Map<String, Map<String, ControlResultData>>): List<SaveOperation>

    // Metoda do zapisu formularza
    private fun saveForm(): Boolean {
        // Waliduj formularz
        if (!validateForm()) {
            return false
        }

        // Zbierz dane
        val rawFormData = collectFormData()

        // Przetwórz dane według logiki konkretnego formularza
        val databaseOperations = processFormData(rawFormData)

        try {
            // Zapisz do bazy danych
            DatabaseManager.updateDatabase(databaseOperations)
            return true
        } catch (e: Exception) {
            println("Błąd zapisu formularza: ${e.message}")
            return false
        }
    }

}