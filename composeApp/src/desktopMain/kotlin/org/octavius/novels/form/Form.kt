package org.octavius.novels.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.control.Control
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.navigator.Screen

abstract class Form : Screen {
    // Schema and state
    private val formSchema: FormControls by lazy {
        val schema = createSchema()
        setupParentChildRelationships(schema.controls)
        schema
    }

    private fun setupParentChildRelationships(controls: Map<String, Control<*>>) {
        controls.forEach { control ->
            control.value.setupParentRelationships(control.key, controls)
        }
    }

    protected abstract fun createSchema(): FormControls

    // Inicjalizacja danych
    protected abstract fun initData(): Map<String, Any?>

    private val formState: MutableMap<String, ControlState<*>> = mutableMapOf()

    protected abstract fun defineTableRelations(): List<TableRelation>

    // Loading data

    protected var loadedId: Int? = null

    fun loadData(id: Int) {
        // Najpierw pobierz dane z inicjalizacji
        val initValues = initData()

        // Potem pobierz dane z bazy
        val tableRelations = defineTableRelations()
        val databaseData = DatabaseManager.getEntityWithRelations(id, tableRelations)

        for ((controlName, control) in formSchema.controls) {
            val value = when {
                // Priorytet dla danych z initData()
                initValues.containsKey(controlName) -> initValues[controlName]
                // Jeśli nie ma w init, to z bazy danych
                control.columnInfo != null -> databaseData[control.columnInfo]
                // W ostateczności null
                else -> null
            }
            formState[controlName] = control.setInitValue(value)
        }
        loadedId = id
    }

    fun clearForm() {
        val initValues = initData()

        for ((controlName, control) in formSchema.controls) {
            val value = when {
                // Priorytet dla danych z initData()
                initValues.containsKey(controlName) -> initValues[controlName]
                // W ostateczności null
                else -> null
            }
            formState[controlName] = control.setInitValue(value)
        }
        loadedId = null
    }

    // Display
    @Composable
    override fun Content(paddingValues: PaddingValues) {
        val navigator = LocalNavigator.current
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState) // Dodajemy przewijanie pionowe
        ) {
            formSchema.order.forEach {
                formSchema.controls[it]?.Render(formState[it]!!, formSchema.controls, formState)
            }

            // Przyciski akcji na dole formularza
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
                                navigator.showSnackbar("Formularz został zapisany pomyślnie")
                                // Wróć do poprzedniego ekranu po zapisie
                                navigator.removeScreen()
                            } else {
                                navigator.showSnackbar("Formularz zawiera błędy")
                            }
                        } catch (e: Exception) {
                            navigator.showSnackbar("Wystąpił błąd: ${e.message}")
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
    }

    // Validate
    private fun validateForm(): Boolean {
        var isValid = true

        for ((controlName, control) in formSchema.controls) {
            // Pobierz stan kontrolki
            val state = formState[controlName]!!
            state.error.value = null
            control.validateControl(controlName, state, formSchema.controls, formState)

            if (state.error.value != null) {
                isValid = false
            }
        }

        return isValid
    }

    // Save
    private fun collectFormData(): Map<String, ControlResultData> {
        val result = mutableMapOf<String, ControlResultData>()

        for ((controlName, control) in formSchema.controls) {
            val state = formState[controlName]!!
            val value = control.getResult(state, formSchema.controls, formState)

            // Dodaj wartość
            result[controlName] = ControlResultData(value, state.dirty.value)
        }

        return result
    }

    // Metoda do przetwarzania danych przed zapisem
    protected abstract fun processFormData(formData: Map<String, ControlResultData>): List<SaveOperation>

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